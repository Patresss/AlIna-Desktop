package com.patres.alina.server.integration.mail.send;

import com.patres.alina.common.card.State;
import com.patres.alina.server.integration.Integration;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationSettings;

import java.util.Map;

public class SendMailIntegrationSettings extends AlinaIntegrationSettings {


    final static String SENDER_EMAIL_ADDRESS = "senderEmailAddress";
    final static String RECEIVER_EMAIL_ADDRESS = "receiverEmailAddress";
    final static String PASSWORD = "password";
    final static String HOST = "host";
    final static String PORT = "port";

    private final String senderEmailAddress;
    private final String receiverEmailAddress;
    private final String password;
    private final String host;
    private final int port;

    public SendMailIntegrationSettings(Integration integration) {
        this(integration.id(), integration.state(), integration.integrationSettings());
    }

    public SendMailIntegrationSettings(String id, State state, Map<String, Object> settings) {
        super(id, state);
        this.senderEmailAddress = getSettingsStringValue(settings, SENDER_EMAIL_ADDRESS);
        this.receiverEmailAddress = getSettingsStringValue(settings, RECEIVER_EMAIL_ADDRESS);
        this.password = getSettingsMandatoryStringValue(settings, PASSWORD);
        this.host = getSettingsMandatoryStringValue(settings, HOST);
        this.port = getSettingsMandatoryIntValue(settings, PORT);
    }

    public String getSenderEmailAddress() {
        return senderEmailAddress;
    }

    public String getPassword() {
        return password;
    }

    public String getReceiverEmailAddress() {
        return receiverEmailAddress;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
