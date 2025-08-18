package com.patres.alina.server.integration.homeassistant;

import com.patres.alina.common.field.FormField;
import com.patres.alina.common.field.FormFieldType;
import com.patres.alina.common.field.UiForm;
import com.patres.alina.server.integration.Integration;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationFunction;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationType;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.patres.alina.server.integration.homeassistant.HomeAssistantIntegrationSettings.*;

@Component
public class HomeAssistantIntegrationType extends AlinaIntegrationType<HomeAssistantIntegrationSettings, HomeAssistantExecutor> {

    private static final String INTEGRATION_TYPE_NAME = "homeAssistant";
    private static final String INTEGRATION_DEFAULT_NAME_TO_DISPLAY = "Home Assistant";
    private static final String INTEGRATION_DESCRIPTION = """
            Funkcja jest przeznaczona do komunikacji z Home Assistant poprzez wysyłanie wiadomości dp Home Assistant dotyczących zarządzania inteligentnym domem.
            Zazwyczaj odnosi się do pomieszczeń np. salonu, balkonu, pokoju dzieci, biura, kuchni, przedpokoju, pralni, łazienki, balkonu w salonie, balkonu w sypialnie
            Często steruje takimi rzeczami jak: jak kontrola temperatury, pomiar wilgotności, włączanie i wyłączanie oświetlenia, zmiana koloru oświetlenia, zarządzanie roletami, zarządzaniem multimediów, klimatyzacją, czajnikiem, a także inne czynności związane z inteligentnym domem.
            Staraj się nie parafrazować komend, tylko wygeneruj podobną komendą na podstawie prompta w takim samym jęyzku w jakim zostało wydane polecenie. Jak np. spytam "Jaka jest temperatura w salonie" to komenda też ma być "Jaka jest temperatura w salonie", a np. "Sprawdź temperaturę w biurze"
            """;
    private static final String ICON = "fth-home";

    private static final UiForm UI_FORM = new UiForm(List.of(
            new FormField(HOME_ASSISTANT_BASE_URL, INTEGRATION_TYPE_NAME + ".homeAssistantBaseUrl.name", "Home Assistant Url", null, null, true, FormFieldType.TEXT_FIELD),
            new FormField(HOME_ASSISTANT_TOKEN, INTEGRATION_TYPE_NAME + ".homeAssistantToken.name", "Home Assistant Token", null, null, true, FormFieldType.TEXT_FIELD),
            new FormField(ALTERNATIVE_AGENT_ID_KEY, INTEGRATION_TYPE_NAME + ".alternativeAgentId.name", "Alternative Agent Id", INTEGRATION_TYPE_NAME + ".alternativeAgentId.description", "Alternate Agent. Used when the default agent does not return a response", false, FormFieldType.TEXT_FIELD)
    ));

    public HomeAssistantIntegrationType() {
        super(INTEGRATION_TYPE_NAME, INTEGRATION_DEFAULT_NAME_TO_DISPLAY, INTEGRATION_DESCRIPTION, ICON, UI_FORM,
                List.of(
                        new AlinaIntegrationFunction<>("", "", HomeAssistantExecutor::handleResponse, HomeAssistantFunctionRequest.class)
                ));
    }

    @Override
    public HomeAssistantExecutor createExecutor(HomeAssistantIntegrationSettings settings) {
        return new HomeAssistantExecutor(settings);
    }

    @Override
    public HomeAssistantIntegrationSettings createSettings(Integration integration) {
        return new HomeAssistantIntegrationSettings(integration);
    }
}
