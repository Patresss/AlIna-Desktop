package com.patres.alina.uidesktop.settings;

public record ServerSettings(
        String serverAddress,
        String serverPassword
) {

    public ServerSettings() {
        this(null, null);
    }

    public ServerSettings(String serverAddress,
                          String serverPassword) {
        this.serverAddress = serverAddress;
        this.serverPassword = serverPassword;
    }
}