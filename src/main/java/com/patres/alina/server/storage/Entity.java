package com.patres.alina.server.storage;

/**
 * Marker interface for entities that can be stored locally
 */
public interface Entity<ID> {
    
    /**
     * Get unique identifier for this entity
     */
    ID getId();
}