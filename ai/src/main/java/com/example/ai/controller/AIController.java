package com.example.ai.controller;

import com.example.ai.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
