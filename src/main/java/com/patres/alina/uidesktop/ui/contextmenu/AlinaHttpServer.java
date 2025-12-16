package com.patres.alina.uidesktop.ui.contextmenu;

import com.patres.alina.server.command.Command;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.shortcuts.CommandExecutor;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Tiny local HTTP server to trigger the context menu when shortcuts cannot be simulated.
 * No external dependencies: uses the built-in JDK HttpServer.
 */
public final class AlinaHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(AlinaHttpServer.class);
    private static final int DEFAULT_PORT = 12137;
    private static final String PORT_PROPERTY = "context.menu.http.port";
    private static final String HOST_NAME = "127.0.0.1";
    private static final String CONTEXT_MENU_PATH = "/context-menu";
    private static final String COMMANDS_CONTEXT_PATH = "/commands";
    private static final String COMMANDS_SEGMENT = "commands";
    private static final String COPY_AND_PASTE_SEGMENT = "copy-and-paste";
    private static final String CONTENT_TYPE_TEXT = "text/plain; charset=utf-8";
    private static final Set<HttpMethod> DEFAULT_ALLOWED_METHODS = Set.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.HEAD);
    private static final String UNKNOWN_ERROR = "unknown error";

    private AlinaHttpServer() {
    }

    public static void start(ApplicationWindow applicationWindow, AppGlobalContextMenu contextMenu) {
        final int port = resolvePort();
        try {
            final HttpServer server = createServer(applicationWindow, contextMenu, port);
            server.start();
            logEndpoints(port);
        } catch (IOException e) {
            logger.error("Cannot start context menu HTTP server on port {}", port, e);
        }
    }

    private static HttpServer createServer(ApplicationWindow applicationWindow, AppGlobalContextMenu contextMenu, int port) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(HOST_NAME, port), 0);
        server.createContext(CONTEXT_MENU_PATH, new DisplayMenuHandler(contextMenu));
        server.createContext(COMMANDS_CONTEXT_PATH, new CommandCopyAndPasteHandler(new CommandExecutor(applicationWindow)));
        server.setExecutor(createExecutor());
        return server;
    }

    private static int resolvePort() {
        final Integer configuredPort = Integer.getInteger(PORT_PROPERTY);
        if (configuredPort == null) {
            return DEFAULT_PORT;
        }
        if (configuredPort <= 0 || configuredPort > 65_535) {
            logger.warn("Ignoring invalid port {} from system property {}, falling back to {}", configuredPort, PORT_PROPERTY, DEFAULT_PORT);
            return DEFAULT_PORT;
        }
        return configuredPort;
    }

    private static Executor createExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "context-menu-http");
            thread.setDaemon(true);
            return thread;
        });
    }

    private static void logEndpoints(int port) {
        logger.info("Context menu HTTP endpoint listening on http://{}:{}{}", HOST_NAME, port, CONTEXT_MENU_PATH);
        logger.info("Command trigger HTTP endpoint listening on http://{}:{}/commands/{id}/{}", HOST_NAME, port, COPY_AND_PASTE_SEGMENT);
    }

    private static boolean isMethodAllowedOrRespond(HttpExchange exchange, Set<HttpMethod> allowedMethods) throws IOException {
        final Optional<HttpMethod> httpMethod = parseHttpMethod(exchange.getRequestMethod());
        if (httpMethod.isPresent() && allowedMethods.contains(httpMethod.get())) {
            return true;
        }
        exchange.getResponseHeaders().set("Allow", allowedMethods.stream().map(HttpMethod::name).collect(Collectors.joining(", ")));
        sendStatusOnly(exchange, HttpURLConnection.HTTP_BAD_METHOD);
        return false;
    }

    private static Optional<HttpMethod> parseHttpMethod(String method) {
        if (method == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(HttpMethod.valueOf(method.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            logger.warn("Unsupported HTTP method received: {}", method);
            return Optional.empty();
        }
    }

    private static void sendStatusOnly(HttpExchange exchange, int statusCode) throws IOException {
        try {
            exchange.sendResponseHeaders(statusCode, 0);
        } finally {
            exchange.close();
        }
    }

    private static void sendPlainTextResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        final byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_TEXT);
        try (exchange) {
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            if (!isHead(exchange)) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        }
    }

    private static boolean isHead(HttpExchange exchange) {
        final String method = exchange.getRequestMethod();
        return HttpMethod.HEAD.name().equalsIgnoreCase(method);
    }

    private record DisplayMenuHandler(AppGlobalContextMenu contextMenu) implements HttpHandler {

        private static final Set<HttpMethod> ALLOWED_METHODS = DEFAULT_ALLOWED_METHODS;

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isMethodAllowedOrRespond(exchange, ALLOWED_METHODS)) {
                return;
            }

            if (isHead(exchange)) {
                sendPlainTextResponse(exchange, HttpURLConnection.HTTP_OK, "Context menu endpoint available");
                return;
            }

            try {
                contextMenu.displayContextMenu();
                sendPlainTextResponse(exchange, HttpURLConnection.HTTP_OK, "Context menu triggered");
            } catch (Exception e) {
                logger.error("Failed to display context menu", e);
                final String response = "Failed to display context menu: " + Optional.ofNullable(e.getMessage()).orElse(UNKNOWN_ERROR);
                sendPlainTextResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, response);
            }
        }
    }

    private record CommandCopyAndPasteHandler(CommandExecutor commandExecutor) implements HttpHandler {

        private static final Set<HttpMethod> ALLOWED_METHODS = DEFAULT_ALLOWED_METHODS;

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isMethodAllowedOrRespond(exchange, ALLOWED_METHODS)) {
                return;
            }

            final Optional<String> commandIdOptional = extractCommandId(exchange.getRequestURI().getPath());
            if (commandIdOptional.isEmpty()) {
                sendStatusOnly(exchange, HttpURLConnection.HTTP_NOT_FOUND);
                return;
            }

            final String commandId = commandIdOptional.get();
            final Optional<Command> commandOpt = BackendApi.getEnabledCommands().stream()
                    .filter(cmd -> cmd.id().equals(commandId))
                    .findFirst();

            if (commandOpt.isEmpty()) {
                final String response = "Command not found: " + commandId;
                sendPlainTextResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, response);
                return;
            }

            if (isHead(exchange)) {
                sendPlainTextResponse(exchange, HttpURLConnection.HTTP_OK, "Command endpoint available");
                return;
            }

            try {
                commandExecutor.executeWithSelectedText(commandOpt.get());
                sendPlainTextResponse(exchange, HttpURLConnection.HTTP_OK, "Command triggered: " + commandId);
            } catch (Exception e) {
                logger.error("Failed to execute command {}", commandId, e);
                final String response = "Failed to execute command: " + Optional.ofNullable(e.getMessage()).orElse(UNKNOWN_ERROR);
                sendPlainTextResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, response);
            }
        }

        private Optional<String> extractCommandId(String path) {
            if (path == null) {
                logger.warn("Received null path for command endpoint");
                return Optional.empty();
            }
            final String normalizedPath = normalizePath(path);
            final String[] parts = normalizedPath.split("/");
            if (parts.length != 3 || !COMMANDS_SEGMENT.equals(parts[0]) || !COPY_AND_PASTE_SEGMENT.equals(parts[2])) {
                return Optional.empty();
            }
            if (parts[1].isBlank()) {
                return Optional.empty();
            }

            try {
                return Optional.of(URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid command id encoding in path {}", path, e);
                return Optional.empty();
            }
        }

        private String normalizePath(String path) {
            String normalized = path;
            while (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            while (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            return normalized;
        }
    }
}
