package com.patres.alina.server.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JSONL (JSON Lines) repository implementation.
 * Each entity is stored as one line of JSON in a file.
 * Perfect for append operations - new records are simply added to the end of file.
 * 
 * @param <T> Entity type
 * @param <ID> ID type
 */
public class JsonLinesRepository<T extends Entity<ID>, ID> implements LocalRepository<T, ID> {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonLinesRepository.class);
    
    private final Path jsonlFile;
    private final Class<T> entityClass;
    private final ObjectMapper objectMapper;
    
    public JsonLinesRepository(Path jsonlFile, Class<T> entityClass, ObjectMapper objectMapper) {
        this.jsonlFile = jsonlFile;
        this.entityClass = entityClass;
        this.objectMapper = objectMapper;
        
        try {
            // Create parent directories if needed
            Files.createDirectories(jsonlFile.getParent());
        } catch (IOException e) {
            throw new LocalStorageException("Failed to create directory for: " + jsonlFile, e);
        }
        
        logger.debug("Initialized JSONL repository for {} at {}", entityClass.getSimpleName(), jsonlFile);
    }
    
    @Override
    public void save(T entity) {
        if (entity == null || entity.getId() == null) {
            throw new IllegalArgumentException("Entity and ID cannot be null");
        }
        
        try {
            // Check if entity already exists - if so, we need to rewrite the file
            if (existsById(entity.getId())) {
                replaceEntity(entity);
            } else {
                appendEntity(entity);
            }
            
            logger.debug("Saved {} with ID {} to {}", entityClass.getSimpleName(), entity.getId(), jsonlFile);
            
        } catch (IOException e) {
            throw new LocalStorageException("Failed to save entity with ID: " + entity.getId(), e);
        }
    }
    
    @Override
    public Optional<T> findById(ID id) {
        if (id == null) {
            return Optional.empty();
        }
        
        if (!Files.exists(jsonlFile)) {
            return Optional.empty();
        }
        
        try (BufferedReader reader = Files.newBufferedReader(jsonlFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                try {
                    T entity = objectMapper.readValue(line, entityClass);
                    if (id.equals(entity.getId())) {
                        logger.debug("Found {} with ID {} in {}", entityClass.getSimpleName(), id, jsonlFile);
                        return Optional.of(entity);
                    }
                } catch (IOException e) {
                    logger.warn("Failed to parse line in {}: {}", jsonlFile, line, e);
                }
            }
            
        } catch (IOException e) {
            logger.error("Failed to read from {}", jsonlFile, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<T> findAll() {
        List<T> entities = new ArrayList<>();
        
        if (!Files.exists(jsonlFile)) {
            return entities;
        }
        
        try (BufferedReader reader = Files.newBufferedReader(jsonlFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                try {
                    T entity = objectMapper.readValue(line, entityClass);
                    entities.add(entity);
                } catch (IOException e) {
                    logger.warn("Failed to parse line in {}: {}", jsonlFile, line, e);
                }
            }
            
        } catch (IOException e) {
            throw new LocalStorageException("Failed to read from: " + jsonlFile, e);
        }
        
        logger.debug("Found {} entities of type {} in {}", entities.size(), entityClass.getSimpleName(), jsonlFile);
        return entities;
    }
    
    @Override
    public void deleteById(ID id) {
        if (id == null || !Files.exists(jsonlFile)) {
            return;
        }
        
        try {
            List<T> allEntities = findAll();
            allEntities.removeIf(entity -> id.equals(entity.getId()));
            
            // Rewrite file without the deleted entity
            rewriteFile(allEntities);
            
            logger.debug("Deleted {} with ID {} from {}", entityClass.getSimpleName(), id, jsonlFile);
            
        } catch (IOException e) {
            throw new LocalStorageException("Failed to delete entity with ID: " + id, e);
        }
    }
    
    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }
    
    @Override
    public void deleteAll() {
        try {
            Files.deleteIfExists(jsonlFile);
            logger.info("Deleted all {} entities from {}", entityClass.getSimpleName(), jsonlFile);
        } catch (IOException e) {
            throw new LocalStorageException("Failed to delete all entities from: " + jsonlFile, e);
        }
    }
    
    @Override
    public long count() {
        if (!Files.exists(jsonlFile)) {
            return 0;
        }
        
        try (BufferedReader reader = Files.newBufferedReader(jsonlFile)) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .count();
        } catch (IOException e) {
            throw new LocalStorageException("Failed to count entities in: " + jsonlFile, e);
        }
    }
    
    /**
     * Append new entity to the end of file - this is the fast path for new entities
     */
    private void appendEntity(T entity) throws IOException {
        String jsonLine = objectMapper.writeValueAsString(entity);
        
        try (BufferedWriter writer = Files.newBufferedWriter(jsonlFile, 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(jsonLine);
            writer.newLine();
        }
    }
    
    /**
     * Replace existing entity - requires rewriting the file
     */
    private void replaceEntity(T entity) throws IOException {
        List<T> allEntities = findAll();
        
        // Replace the entity with the same ID
        for (int i = 0; i < allEntities.size(); i++) {
            if (entity.getId().equals(allEntities.get(i).getId())) {
                allEntities.set(i, entity);
                break;
            }
        }
        
        rewriteFile(allEntities);
    }
    
    /**
     * Rewrite entire file with given entities
     */
    private void rewriteFile(List<T> entities) throws IOException {
        // Write to temp file first for atomicity
        Path tempFile = Files.createTempFile(jsonlFile.getParent(), "tmp_", ".jsonl");
        
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            for (T entity : entities) {
                String jsonLine = objectMapper.writeValueAsString(entity);
                writer.write(jsonLine);
                writer.newLine();
            }
        }
        
        // Atomic move
        Files.move(tempFile, jsonlFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}