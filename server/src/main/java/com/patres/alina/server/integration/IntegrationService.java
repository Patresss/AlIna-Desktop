package com.patres.alina.server.integration;


import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.State;
import com.patres.alina.common.card.UpdateStateRequest;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.integration.*;
import com.patres.alina.server.event.AssistantIntegrationsUpdatedEvent;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationType;
import com.patres.alina.server.integration.exception.IntegrationTypeNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.patres.alina.server.integration.IntegrationMapper.*;
import static java.util.stream.Collectors.toMap;

@Service
public class IntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationService.class);


    private static final String DEFAULT_ICON = "mdal-integration_instructions";
    private final IntegrationRepository integrationRepository;
    private final List<AlinaIntegrationType<?, ?>> availableIntegrations;
    private final Map<String, AlinaIntegrationType<?, ?>> integrationByName;

    public IntegrationService(IntegrationRepository integrationRepository, List<AlinaIntegrationType<?, ?>> availableIntegrations) {
        this.integrationRepository = integrationRepository;
        this.availableIntegrations = availableIntegrations.stream()
                .sorted(Comparator.comparing(AlinaIntegrationType::getTypeName))
                .toList();
        this.integrationByName = availableIntegrations.stream()
                .collect(toMap(AlinaIntegrationType::getTypeName, Function.identity()));
    }

    public List<IntegrationToAdd> getAvailableIntegrationToAdd() {
        return availableIntegrations.stream()
                .map(IntegrationMapper::toIntegrationToAdd)
                .toList();
    }

    public List<CardListItem> getIntegrationListItem() {
        return integrationRepository.findAll().stream()
                .filter(this::isSupportedIntegration)
                .map(it -> toIntegrationListItem(it, getIcon(it)))
                .toList();
    }


    public Optional<IntegrationDetails> getIntegrationById(final String id) {
        return integrationRepository.findById(id)
                .map(it -> toIntegrationDetails(it, integrationByName.get(it.integrationType())));
    }

    public IntegrationDetails getNewIntegrationByIntegrationType(final String integrationType) {
        if (!integrationByName.containsKey(integrationType)) {
            throw new IntegrationTypeNotFoundException(integrationType);
        }
        return toIntegrationDetails(integrationByName.get(integrationType));
    }

    public IntegrationSaved createIntegration(final IntegrationCreateRequest integrationCreateRequest) {
        logger.info("Creating a new integration: {}", integrationCreateRequest);
        final Integration integration = toIntegration(integrationCreateRequest);
        final Integration createdIntegration = integrationRepository.save(integration);
        logger.info("Created the integration with id=`{}`", createdIntegration.id());

        return handleSavedIntegration(integration);
    }

    public IntegrationSaved updateIntegration(final IntegrationUpdateRequest integrationUpdateRequest) {
        logger.info("Updating the integration: {}", integrationUpdateRequest);
        final Integration integration = toIntegration(integrationUpdateRequest);
        final Integration updatedIntegration = integrationRepository.save(integration);
        logger.info("Updated the integration with id=`{}`", updatedIntegration.id());

        return handleSavedIntegration(integration);
    }

    private IntegrationSaved handleSavedIntegration(Integration integration) {
        final boolean authNeeded = getAlinaIntegrationType(integration)
                .map(alinaIntegrationType -> alinaIntegrationType.isAuthNeeded(integration))
                .orElse(false);

        if (authNeeded) {
            final String authLink = getAlinaIntegrationType(integration)
                    .flatMap(alinaIntegrationType -> alinaIntegrationType.getAuthLink(integration))
                    .orElse(null);
            return new IntegrationSaved(integration.id(), authLink);
        }

        publishIntegrationUpdateEvent();
        return new IntegrationSaved(integration.id());
    }

    public void updateIntegrationState(final UpdateStateRequest updateStateRequest) {
        logger.info("Setting integration {} state to {}...", updateStateRequest.id(), updateStateRequest.state());
        integrationRepository.updateStateById(updateStateRequest.id(), updateStateRequest.state());
        publishIntegrationUpdateEvent();
    }

    public void deleteIntegration(final String id) {
        logger.info("Deleting integration: '{}'", id);
        integrationRepository.deleteById(id);
        publishIntegrationUpdateEvent();
    }

    private static void publishIntegrationUpdateEvent() {
        DefaultEventBus.getInstance().publish(new AssistantIntegrationsUpdatedEvent());
    }

    public Map<Integration, AlinaIntegrationType<?, ?>> getAllEnabledIntegrations() {
        return integrationRepository.findByState(State.ENABLED).stream()
                .filter(this::isSupportedIntegration)
                .collect(toMap(
                        Function.identity(),
                        integration -> integrationByName.get(integration.integrationType()
                        )));
    }

    private String getIcon(Integration integration) {
        return getAlinaIntegrationType(integration)
                .map(AlinaIntegrationType::getIcon)
                .orElse(DEFAULT_ICON);
    }

    private Optional<AlinaIntegrationType<?, ?>> getAlinaIntegrationType(Integration integration) {
        return Optional.ofNullable(integration)
                .map(Integration::integrationType)
                .map(integrationByName::get);
    }

    private boolean isSupportedIntegration(Integration integration) {
        if (integrationByName.containsKey(integration.integrationType())) {
            return true;
        }
        logger.warn("Integration type {} is not supported", integration.integrationType());
        return false;
    }

    public void updateIntegrationAuth(String integrationId, String code) {
        logger.info("Received Spotify callback for integration {}", integrationId);
        integrationRepository.findById(integrationId)
                .ifPresentOrElse(
                        integration -> updateIntegrationAuth(integration, code),
                        () -> logger.error("Cannot find integration {}", integrationId));
    }

    private void updateIntegrationAuth(Integration integration, String code) {
        getAlinaIntegrationType(integration)
                .ifPresent(type -> {
                    type.setAuth(integration, code);
                    integrationRepository.save(integration);
                    publishIntegrationUpdateEvent();
                });
    }
}
