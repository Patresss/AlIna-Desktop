package com.patres.alina.uidesktop.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.patres.alina.common.exception.ErrorResponse;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ErrorResponseReceiver {

    private static final Logger logger = LoggerFactory.getLogger(ErrorResponseReceiver.class);

    private final static ObjectMapper mapper = createMapper();

    public static String getErrorResponseMessageIfPossible(FeignException feignException) {
        try {
            return feignException.responseBody()
                    .map(ErrorResponseReceiver::toByteArray)
                    .map(ErrorResponseReceiver::getErrorResponse)
                    .map(ErrorResponse::message)
                    .orElse(feignException.getMessage());
        } catch (Exception e) {
            logger.warn("Cannot receive error response", e);
            return feignException.getMessage();
        }
    }

    private static ErrorResponse getErrorResponse(byte[] it) {
        try {
            return mapper.readValue(it, ErrorResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] toByteArray(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private static ObjectMapper createMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }
}