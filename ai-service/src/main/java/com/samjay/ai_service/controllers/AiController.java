package com.samjay.ai_service.controllers;

import com.samjay.ai_service.services.interfaces.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("NullableProblems")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam String userPrompt) {

        String aiResponse = aiService.sendMessage(userPrompt);

        return ResponseEntity.ok(aiResponse);

    }
}
