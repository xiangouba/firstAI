package com.example.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

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

    /*
    * 流式响应与实时输出
    * */

    public Flux<String> streamChat(String message){
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }

    /*
    * 提示工程与模板 — 使用 PromptTemplate 做模板化提示
    * */

    /**
     * 单变量模板：将用户输入注入到预设的提示词模板中
     * 通过 PromptTemplate.create() 将占位符替换为实际值，保证输出格式一致
     */
    public String promptEngineering(String message){
        PromptTemplate template = new PromptTemplate("请用一句话总结一下文本：{input}");
        return chatClient.prompt()
                .user(template.create(Map.of("input", message)).getContents())
                .call()
                .content();
    }

    /**
     * 多变量模板：支持多个占位符，适用于翻译、角色扮演等场景
     */
    public String translateWithTemplate(String text, String targetLang){
        PromptTemplate template = new PromptTemplate(
                "你是一个专业的翻译助手。请将以下文本翻译成{lang}，只返回译文：\n{text}");
        return chatClient.prompt()
                .user(template.create(Map.of("lang", targetLang, "text", text)).getContents())
                .call()
                .content();
    }

    /**
     * 结构化输出模板：引导模型输出 JSON 等结构化格式
     */
    public String extractKeywords(String text){
        PromptTemplate template = new PromptTemplate("""
                从以下文本中提取关键词，以 JSON 数组格式返回：
                {text}
                
                输出格式示例：["关键词1", "关键词2", "关键词3"]
                """);
        return chatClient.prompt()
                .user(template.create(Map.of("text", text)).getContents())
                .call()
                .content();
    }

    /**
     * 结合 System 消息的模板：设置角色 + 用户模板，双重约束提升质量
     */
    public String codeReview(String code){
        PromptTemplate template = new PromptTemplate("请审查以下代码并给出改进建议：\n{code}");
        return chatClient.prompt()
                .system("你是一个资深代码审查专家，请以简洁专业的方式回答。")
                .user(template.create(Map.of("code", code)).getContents())
                .call()
                .content();
    }

    /**
     * AI 智能生成水印文案：根据用户描述的场景，让 AI 推荐合适的水印文字
     */
    public String generateWatermarkText(String scenario){
        PromptTemplate template = new PromptTemplate("""
                根据以下场景，生成一条简短的 PDF 水印文案（10字以内），只返回文案本身，不要加引号：
                场景：{scenario}
                """);
        return chatClient.prompt()
                .system("你是一个文档水印文案生成助手。")
                .user(template.create(Map.of("scenario", scenario)).getContents())
                .call()
                .content();
    }

}
