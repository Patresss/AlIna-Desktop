package com.patres.alina.server.openai.function;

import com.theokanning.openai.completion.chat.ChatFunction;

public abstract class OpenAiFunction {
    public abstract ChatFunction createChatFunction();
}
