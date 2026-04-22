package com.samjay.ai_service.services.implementations;

import com.samjay.ai_service.configurations.AuthenticatedUserProvider;
import com.samjay.ai_service.dtos.UserIdentifier;
import com.samjay.ai_service.services.interfaces.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImplementation implements AiService {

    private final ChatClient chatClient;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Override
    public String sendMessage(String userPrompt) {

        try {

            UserIdentifier userIdentifier = authenticatedUserProvider.getCurrentLoggedInUser();

            log.info("Received message from user: {} (ID: {})", userIdentifier.username(), userIdentifier.userId());

            return chatClient
                    .prompt()
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, userIdentifier.userId()))
                    .user(userPrompt)
                    .call()
                    .content();

        } catch (Exception e) {

            log.error("An error occurred while processing the AI response: {}", e.getMessage());

            return "An error occurred while processing your request. Please try again later.";

        }
    }

    @Override
    public String sendMessageV2(String userPrompt, String userId) {

        try {

            log.info("Received message from user ID: {}", userId);

            return chatClient
                    .prompt()
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, userId))
                    .user(userPrompt)
                    .call()
                    .content();

        } catch (Exception e) {

            log.error("An error occurred while processing the AI response for user ID {}: {}", userId, e.getMessage());

            return "An error occurred while processing your request. Please try again later.";

        }
    }
}
