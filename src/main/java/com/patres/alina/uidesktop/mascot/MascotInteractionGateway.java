package com.patres.alina.uidesktop.mascot;

import com.patres.alina.common.interaction.AgentInteractionResolutionModel;
import com.patres.alina.common.interaction.AgentInteractionResponse;

interface MascotInteractionGateway {

    AgentInteractionResolutionModel resolve(String requestId, AgentInteractionResponse response);

    void retryLastUserMessage(String threadId);
}
