package com.patres.alina.server.storage;

public class LocalStorageException extends RuntimeException {
    
    public LocalStorageException(String message) {
        super(message);
    }
    
    public LocalStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}