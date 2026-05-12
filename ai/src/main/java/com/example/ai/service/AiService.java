package com.example.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiService {
    private final ChatClient chatClient;

    @Autowired
    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 简单对话
     * @param message
     * @return
     */
    public String simpleChat(String message){
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

//    public String
}
