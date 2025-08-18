package com.patres.alina.server.thread;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatThreadRepository extends MongoRepository<ChatThread, String> {

    @Query("{'id' : ?0}")
    @Update("{'$set': {'modifiedAt': ?1}}")
    void updateModifiedAtById(String id, LocalDateTime modifiedAt);

    @Query("{'id' : ?0}")
    @Update("{'$set': {'name': ?1}}")
    void updateNameById(String id, String name);

    @Query(value = "{'modifiedAt': {$ne: null}}", sort = "{'modifiedAt': -1}")
    List<ChatThread> findAvailableChatThreads();

}