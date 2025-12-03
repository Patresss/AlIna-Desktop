package com.patres.alina.uidesktop.common.event;

import com.patres.alina.common.event.Event;

public class CommandShortcutExecutedEvent extends Event {

    private final String threadId;
    private final String commandName;

    public CommandShortcutExecutedEvent(String threadId, String commandName) {
        this.threadId = threadId;
        this.commandName = commandName;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getCommandName() {
        return commandName;
    }
}
