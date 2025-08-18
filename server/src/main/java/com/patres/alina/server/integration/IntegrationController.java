package com.patres.alina.server.integration;


import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.UpdateStateRequest;
import com.patres.alina.common.integration.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "/integrations")
public class IntegrationController {

    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @GetMapping("/available")
    public List<IntegrationToAdd> getAvailableIntegrationToAdd() {
        return integrationService.getAvailableIntegrationToAdd();
    }

    @GetMapping
    public List<CardListItem> getIntegrationListItem() {
        return integrationService.getIntegrationListItem();
    }

    @GetMapping("/{id}")
    public Optional<IntegrationDetails> getIntegrationById(@PathVariable final String id) {
        return integrationService.getIntegrationById(id);
    }

    @GetMapping("/type/{integrationType}")
    public IntegrationDetails getNewIntegrationByIntegrationType(@PathVariable final String integrationType) {
        return integrationService.getNewIntegrationByIntegrationType(integrationType);
    }

    @PostMapping
    public IntegrationSaved createIntegration(@RequestBody final IntegrationCreateRequest integrationCreateRequest) {
        return integrationService.createIntegration(integrationCreateRequest);
    }

    @PutMapping
    public IntegrationSaved updateIntegration(@RequestBody final IntegrationUpdateRequest integrationUpdateRequest) {
        return integrationService.updateIntegration(integrationUpdateRequest);
    }

    @DeleteMapping("/{id}")
    public void deleteChatThread(@PathVariable final String id) {
        integrationService.deleteIntegration(id);
    }

    @PatchMapping("/state")
    public void updateIntegrationState(@RequestBody UpdateStateRequest updateStateRequest) {
        integrationService.updateIntegrationState(updateStateRequest);
    }


}
