//package com.patres.alina.server.openai.function;
//
//import com.patres.alina.common.event.bus.DefaultEventBus;
//import com.patres.alina.server.event.AssistantIntegrationsUpdatedEvent;
//import com.patres.alina.server.integration.IntegrationService;
//import com.theokanning.openai.completion.chat.ChatFunction;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static com.patres.alina.server.openai.function.ChatFunctionCreator.createChatFunctions;
//
//@Service
//public class FunctionService {
//
//    private final IntegrationService integrationService;
//    private List<ChatFunction> chatFunctions = new ArrayList<>();
//
//    public FunctionService(IntegrationService integrationService) {
//        this.integrationService = integrationService;
//        DefaultEventBus.getInstance().subscribe(AssistantIntegrationsUpdatedEvent.class, e -> loadChatFunctions());
//        loadChatFunctions();
//    }
//
//    public List<ChatFunction> getChatFunctions() {
//        return chatFunctions;
//    }
//
//    public void loadChatFunctions() { // TODO, nie laduj wszystkeio tlyko to co sie zmienilo
//        this.chatFunctions = integrationService.getAllEnabledIntegrations().entrySet().stream()
//                .flatMap(entry -> createChatFunctions(entry.getValue(), entry.getKey()).stream())
//                .toList();
//    }
//
//}
