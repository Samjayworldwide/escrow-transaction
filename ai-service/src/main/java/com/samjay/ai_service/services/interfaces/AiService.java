package com.samjay.ai_service.services.interfaces;

public interface AiService {

    String sendMessage(String userPrompt);

    String sendMessageV2(String userPrompt, String userId);

}
