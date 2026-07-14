package com.patres.alina.server.assistant;

import com.patres.alina.common.interaction.AgentInteractionResolutionModel;
import com.patres.alina.common.interaction.AgentInteractionResponse;
import com.patres.alina.server.agent.AgentRuntime;
import com.patres.alina.server.agent.AgentRuntimeSelector;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import org.springframework.stereotype.Component;

@Component
public class AgentInteractionController {

    private final AgentRuntimeSelector agentRuntimeSelector;

    public AgentInteractionController(final AgentRuntimeSelector agentRuntimeSelector) {
        this.agentRuntimeSelector = agentRuntimeSelector;
    }

    public AgentInteractionResolutionModel resolve(final String requestId,
                                                   final AgentInteractionResponse response) {
        for (final AgentRuntime runtime : agentRuntimeSelector.all()) {
            if (runtime.ownsAgentInteraction(requestId)) {
                return runtime.resolveAgentInteraction(requestId, response);
            }
        }
        return AgentInteractionResolutionModel.missing(
                LanguageManager.getLanguageString("chat.interaction.missing")
        );
    }
}
