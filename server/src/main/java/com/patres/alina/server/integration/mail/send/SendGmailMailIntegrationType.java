package com.patres.alina.server.integration.mail.send;

import com.patres.alina.common.field.FormField;
import com.patres.alina.common.field.FormFieldType;
import com.patres.alina.common.field.UiForm;
import com.patres.alina.server.integration.Integration;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationFunction;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.patres.alina.server.integration.mail.send.SendMailIntegrationSettings.*;

@Component
public class SendGmailMailIntegrationType extends AlinaIntegrationType<SendMailIntegrationSettings, SendMailExecutor> {

    private static final String INTEGRATION_TYPE_NAME = "sendGmailEmail";
    private static final String INTEGRATION_DEFAULT_NAME_TO_DISPLAY = "Send Gmail email";
    private static final String INTEGRATION_DESCRIPTION = """
            Send an email""";
    private static final String ICON = "mdmz-mail_outline";

    private static final UiForm UI_FORM = new UiForm(List.of(
            new FormField(SENDER_EMAIL_ADDRESS, INTEGRATION_TYPE_NAME + ".senderEmailAddress.name", "Sender email address", null, null, true, FormFieldType.TEXT_FIELD),
            new FormField(RECEIVER_EMAIL_ADDRESS, INTEGRATION_TYPE_NAME + ".receiverEmailAddress.name", "Receiver email address", null, null, true, FormFieldType.TEXT_FIELD),
            new FormField(PASSWORD, INTEGRATION_TYPE_NAME + "password.name", "Password", null, null, true, FormFieldType.TEXT_FIELD)
    ));

    private static final String GMAIL_HOST = "smtp.gmail.com";
    private static final int GMAIL_PORT = 587;


    public SendGmailMailIntegrationType() {
        super(INTEGRATION_TYPE_NAME, INTEGRATION_DEFAULT_NAME_TO_DISPLAY, INTEGRATION_DESCRIPTION, ICON, UI_FORM,
                List.of(
                        new AlinaIntegrationFunction<>("", "", SendMailExecutor::sendEmail, SendMailFunctionRequest.class)
                ));
    }

    @Override
    public SendMailExecutor createExecutor(SendMailIntegrationSettings settings) {
        return new SendMailExecutor(settings);
    }

    @Override
    public SendMailIntegrationSettings createSettings(Integration integration) {
        final Map<String, Object> settings = integration.integrationSettings();
        settings.put(HOST, GMAIL_HOST);
        settings.put(PORT, GMAIL_PORT);
        return new SendMailIntegrationSettings(integration.id(), integration.state(), settings);
    }
}
