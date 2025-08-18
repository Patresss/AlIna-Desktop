package com.patres.alina.server.integration.alinaintegration;

import com.patres.alina.common.field.UiForm;
import com.patres.alina.server.integration.Integration;

import java.util.List;
import java.util.Optional;


public abstract class AlinaIntegrationType<S extends AlinaIntegrationSettings, E extends AlinaIntegrationExecutor<S>> {


    private final String typeName;
    private final String defaultNameToDisplay;
    private final String defaultDescription;
    private final String icon;
    private final UiForm uiForm;
    private final List<AlinaIntegrationFunction<E, ?>> integrationFunctionList;

    protected AlinaIntegrationType(String name,
                                   String defaultNameToDisplay,
                                   String defaultDescription,
                                   String icon,
                                   UiForm uiForm,
                                   List<AlinaIntegrationFunction<E, ?>> integrationFunctionList) {
        this.typeName = name;
        this.defaultNameToDisplay = defaultNameToDisplay;
        this.defaultDescription = defaultDescription;
        this.icon = icon;
        this.uiForm = uiForm;
        this.integrationFunctionList = integrationFunctionList;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getDefaultDescription() {
        return defaultDescription;
    }

    public UiForm getUiForm() {
        return uiForm;
    }

    public String getIcon() {
        return icon;
    }

    public String getDefaultNameToDisplay() {
        return defaultNameToDisplay;
    }

    public abstract S createSettings(Integration integration);

    public List<AlinaIntegrationFunction<E, ?>> getIntegrationFunctionList() {
        return integrationFunctionList;
    }

    public Optional<String> getAuthLink(Integration integration) {
        return Optional.empty();
    }

    public Optional<String> getAuthCode(Integration integration) {
        return Optional.empty();
    }

    public boolean isAuthNeeded(Integration integration) {
        return getAuthCode(integration).filter(it -> !it.isBlank()).isEmpty() && getAuthLink(integration).isPresent();
    }

    public void setAuth(Integration integration, String authCode) {
    }

    public abstract E createExecutor(S settings);
}
