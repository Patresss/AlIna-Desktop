package com.patres.alina.server.thread;

import com.patres.alina.common.thread.ChatThreadRenameRequest;
import com.patres.alina.common.thread.ChatThreadResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/chat-threads")

public class ChatThreadController {

    private final ChatThreadService chatThreadService;

    public ChatThreadController(ChatThreadService chatThreadService) {
        this.chatThreadService = chatThreadService;
    }

    @GetMapping
    public List<ChatThreadResponse> getChatThreads() {
        return chatThreadService.getChatThreads();
    }

    @PostMapping("/new")
    public ChatThreadResponse createNewChatThread() {
        return chatThreadService.createNewChatThread();
    }

    @DeleteMapping("/{chatThreadId}")
    public void deleteChatThread(@PathVariable String chatThreadId) {
        chatThreadService.deleteChatThread(chatThreadId);
    }

    @PatchMapping("/name")
    public void renameChatThread(@RequestBody ChatThreadRenameRequest chatThreadRenameRequest) {
        chatThreadService.renameChatThread(chatThreadRenameRequest.id(), chatThreadRenameRequest.name());
    }

}
