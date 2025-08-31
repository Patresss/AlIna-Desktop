package com.patres.alina.server.storage;

import java.util.List;
import java.util.Optional;

/**
 * Generic interface for local file-based storage of entities.
 * 
 * @param <T> Entity type
 * @param <ID> ID type
 */
public interface LocalRepository<T, ID> {
    
    /**
     * Save entity to local storage
     */
    void save(T entity);
    
    /**
     * Find entity by ID
     */
    Optional<T> findById(ID id);
    
    /**
     * Find all entities
     */
    List<T> findAll();
    
    /**
     * Delete entity by ID
     */
    void deleteById(ID id);
    
    /**
     * Check if entity exists by ID
     */
    boolean existsById(ID id);
    
    /**
     * Delete all entities
     */
    void deleteAll();
    
    /**
     * Count all entities
     */
    long count();
}