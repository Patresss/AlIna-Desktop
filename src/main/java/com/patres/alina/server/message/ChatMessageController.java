package com.patres.alina.server.message;

import com.patres.alina.common.event.ChatMessageReceivedEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageSendModel;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/chat-messages")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    public ChatMessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @GetMapping("/{chatThreadId}")
    public List<ChatMessageResponseModel> getMessagesByThreadId(@PathVariable String chatThreadId) {
        return chatMessageService.getMessagesByThreadId(chatThreadId);
    }

    @PostMapping
    public ChatMessageResponseModel sendChatMessages(@RequestBody ChatMessageSendModel chatMessageSendModel) {
        ChatMessageResponseModel chatMessageResponseModel = chatMessageService.sendMessage(chatMessageSendModel);
        DefaultEventBus.getInstance().publish(new ChatMessageReceivedEvent(chatMessageResponseModel));
        return chatMessageResponseModel;
    }

}
