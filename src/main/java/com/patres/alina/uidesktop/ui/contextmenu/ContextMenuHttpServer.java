package com.patres.alina.uidesktop.ui.contextmenu;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * Tiny local HTTP server to trigger the context menu when shortcuts cannot be simulated.
 * No external dependencies: uses the built-in JDK HttpServer.
 */
public final class ContextMenuHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(ContextMenuHttpServer.class);
    private static final int DEFAULT_PORT = 18080;

    private ContextMenuHttpServer() {
    }

    public static void start(AppGlobalContextMenu contextMenu) {
        int port = Integer.getInteger("context.menu.http.port", DEFAULT_PORT);
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            server.createContext("/context-menu", new DisplayMenuHandler(contextMenu));
            server.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "context-menu-http");
                t.setDaemon(true);
                return t;
            }));
            server.start();
            logger.info("Context menu HTTP endpoint listening on http://127.0.0.1:{}/context-menu", port);
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
}
