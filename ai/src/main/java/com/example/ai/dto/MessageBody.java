package com.example.ai.dto;

import lombok.Data;

@Data
public class MessageBody {
    private String message;

    /**
     * 翻译原文
     */
    private String text;

    /**
     * 翻译目标语言
     */
    private String targetLang;
}
