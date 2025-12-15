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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Tiny local HTTP server to trigger the context menu when shortcuts cannot be simulated.
 * No external dependencies: uses the built-in JDK HttpServer.
 */
public final class AlinaHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(AlinaHttpServer.class);
    private static final int DEFAULT_PORT = 18080;

    private AlinaHttpServer() {
    }

    public static void start(ApplicationWindow applicationWindow, AppGlobalContextMenu contextMenu) {
        int port = Integer.getInteger("context.menu.http.port", DEFAULT_PORT);
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            server.createContext("/context-menu", new DisplayMenuHandler(contextMenu));
            server.createContext("/commands", new CommandCopyAndPasteHandler(new CommandExecutor(applicationWindow)));
            server.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "context-menu-http");
                t.setDaemon(true);
                return t;
            }));
            server.start();
            logger.info("Context menu HTTP endpoint listening on http://127.0.0.1:{}/context-menu", port);
            logger.info("Command trigger HTTP endpoint listening on http://127.0.0.1:{}/commands/{id}/copy-and-paste", port);
        } catch (IOException e) {
            logger.error("Cannot start context menu HTTP server on port {}", port, e);
        }
    }

    private static class DisplayMenuHandler implements HttpHandler {
        private final AppGlobalContextMenu contextMenu;

        DisplayMenuHandler(AppGlobalContextMenu contextMenu) {
            this.contextMenu = contextMenu;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Context menu triggered";
            try {
                contextMenu.displayContextMenu();
            } catch (Exception e) {
                logger.error("Failed to display context menu", e);
                response = "Failed to display context menu: " + Optional.ofNullable(e.getMessage()).orElse("unknown error");
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    private static class CommandCopyAndPasteHandler implements HttpHandler {
        private final CommandExecutor commandExecutor;

        CommandCopyAndPasteHandler(CommandExecutor commandExecutor) {
            this.commandExecutor = commandExecutor;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            final String path = exchange.getRequestURI().getPath();
            final String[] parts = path.split("/");
            if (parts.length != 4 || !"commands".equals(parts[1]) || !"copy-and-paste".equals(parts[3])) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            final String commandId = URLDecoder.decode(parts[2], StandardCharsets.UTF_8);
            Optional<Command> commandOpt = BackendApi.getEnabledCommands().stream()
                    .filter(cmd -> cmd.id().equals(commandId))
                    .findFirst();

            if (commandOpt.isEmpty()) {
                String response = "Command not found: " + commandId;
                exchange.sendResponseHeaders(404, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            try {
                commandExecutor.executeWithSelectedText(commandOpt.get());
                String response = "Command triggered: " + commandId;
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } catch (Exception e) {
                logger.error("Failed to execute command {}", commandId, e);
                String response = "Failed to execute command: " + Optional.ofNullable(e.getMessage()).orElse("unknown error");
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
}
