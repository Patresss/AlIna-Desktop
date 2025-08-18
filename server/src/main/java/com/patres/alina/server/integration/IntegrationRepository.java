package com.patres.alina.server.integration;

import com.patres.alina.common.card.State;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.List;

public interface IntegrationRepository extends MongoRepository<Integration, String> {

    @Query("{'id' : ?0}")
    @Update("{'$set': {'state': ?1}}")
    void updateStateById(String id, State state);

    List<Integration> findByState(State state);


}