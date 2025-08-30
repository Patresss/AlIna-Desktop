package com.patres.alina.server.plugin;

import com.patres.alina.common.card.State;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

public interface PluginRepository extends MongoRepository<Plugin, String> {

    @Query("{'id' : ?0}")
    @Update("{'$set': {'state': ?1}}")
    void updateStateById(String id, State state);

}