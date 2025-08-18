package com.patres.alina.server.integration;

import com.patres.alina.common.card.State;
import org.springframework.data.annotation.Id;

import java.util.Map;

public record Integration(
        @Id
        String id,
        String integrationType,
        String name,
        String description,
        State state,
        Map<String, Object> integrationSettings
) {

}