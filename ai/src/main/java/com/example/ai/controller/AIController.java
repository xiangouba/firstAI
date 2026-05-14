package com.example.ai.controller;

import com.example.ai.dto.MessageBody;
import com.example.ai.dto.Result;
import com.example.ai.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {
    private final AiService aiService;

    @Autowired
    public AIController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String,String>> chat(@RequestBody Map<String,String> request){
        String message = request.get("message");
        String s = aiService.simpleChat(message);
        return ResponseEntity.ok(Map.of("message", s));

    }

    @PostMapping("/chatStream")
    public Result chatStream(@RequestBody MessageBody messageBody){
        String message = messageBody.getMessage();
        Flux<String> s = aiService.streamChat(message);
        return Result.success(s);

    }

    @PostMapping("/chatTranslate")
    public Result chatTranslate(@RequestBody MessageBody messageBody){
        String message = messageBody.getMessage();
        String s = aiService.translateWithTemplate( messageBody.getText(), messageBody.getTargetLang());
        return Result.success(s);

    }
}
