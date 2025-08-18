package com.patres.alina.server.integration.alinaintegration;

public abstract class AlinaIntegrationExecutor<S extends AlinaIntegrationSettings> {

    protected final S settings;

    protected AlinaIntegrationExecutor(S settings) {
        this.settings = settings;
    }

}
