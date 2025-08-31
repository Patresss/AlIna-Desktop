package com.patres.alina.server.thread;

import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.common.thread.ChatThreadRenameRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/chat-threads")
public class ChatThreadController {

    private final ChatThreadService chatThreadService;

    public ChatThreadController(final ChatThreadService chatThreadService) {
        this.chatThreadService = chatThreadService;
    }

    @GetMapping
    public List<ChatThread> getChatThreads() {
        return chatThreadService.getChatThreads();
    }

    @PostMapping("/new")
    public ChatThread createNewChatThread() {
        return chatThreadService.createNewChatThread();
    }

    @DeleteMapping("/{chatThreadId}")
    public void deleteChatThread(@PathVariable final String chatThreadId) {
        chatThreadService.deleteChatThread(chatThreadId);
    }

    @PatchMapping("/name")
    public void renameChatThread(@RequestBody final ChatThreadRenameRequest chatThreadRenameRequest) {
        chatThreadService.renameChatThread(chatThreadRenameRequest.id(), chatThreadRenameRequest.name());
    }
}
