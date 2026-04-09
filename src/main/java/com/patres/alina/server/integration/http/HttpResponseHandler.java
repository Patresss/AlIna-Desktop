package com.patres.alina.server.integration.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;

/**
 * Centralized HTTP response handling with standardized logging and error classification.
 */
public final class HttpResponseHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpResponseHandler.class);

    private HttpResponseHandler() {
    }

    /**
     * Checks if the HTTP response indicates success (2xx status code).
     */
    public static boolean isSuccess(final HttpResponse<?> response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    /**
     * Logs and classifies HTTP error responses.
     * Returns a human-readable error description.
     */
    public static String describeError(final String serviceName, final HttpResponse<String> response) {
        final int status = response.statusCode();
        final String body = response.body();

        return switch (status) {
            case 401 -> {
                logger.warn("{}: HTTP 401 Unauthorized. Check your credentials.", serviceName);
                yield "Unauthorized - check credentials";
            }
            case 403 -> {
                logger.warn("{}: HTTP 403 Forbidden. Insufficient permissions.", serviceName);
                yield "Forbidden - insufficient permissions";
            }
            case 404 -> {
                logger.warn("{}: HTTP 404 Not Found.", serviceName);
                yield "Resource not found";
            }
            case 429 -> {
                logger.warn("{}: HTTP 429 Too Many Requests. Rate limit exceeded.", serviceName);
                yield "Rate limit exceeded - try again later";
            }
            default -> {
                if (status >= 500) {
                    logger.warn("{}: HTTP {} Server Error: {}", serviceName, status, body);
                    yield "Server error (HTTP " + status + ")";
                } else {
                    logger.warn("{}: HTTP {} Error: {}", serviceName, status, body);
                    yield "HTTP error " + status;
                }
            }
        };
    }
}
