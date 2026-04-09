package com.patres.alina.server.integration.http;

import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Factory providing shared {@link HttpClient} instances with connection pooling.
 * Instead of creating a new client per request, services should inject this factory
 * and reuse the shared client.
 */
@Component
public class HttpClientFactory {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient sharedClient = HttpClient.newBuilder()
            .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
            .build();

    /**
     * Returns the shared {@link HttpClient} instance with default connect timeout.
     * The client supports HTTP/2 and connection pooling internally.
     */
    public HttpClient getClient() {
        return sharedClient;
    }
}
