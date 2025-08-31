package com.patres.alina.server.message;

import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageSendModel;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/chat-messages")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    public ChatMessageController(final ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @GetMapping("/{chatThreadId}")
    public List<ChatMessageResponseModel> getMessagesByThreadId(@PathVariable final String chatThreadId) {
        return chatMessageService.getMessagesByThreadId(chatThreadId);
    }

    @PostMapping("/stream")
    public void sendChatMessagesStream(@RequestBody final ChatMessageSendModel chatMessageSendModel) {
        chatMessageService.sendMessageStream(chatMessageSendModel);
    }

}
