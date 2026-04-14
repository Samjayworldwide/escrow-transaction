package com.samjay.notification_service.services.implementations;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.samjay.notification_service.dtos.requests.FirebaseNotificationRequest;
import com.samjay.notification_service.services.interfaces.FirebaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FirebaseServiceImplementation implements FirebaseService {

    @Override
    public void sendNotificationToDevice(FirebaseNotificationRequest firebaseNotificationRequest) {

        try {

            Message message = Message.builder()
                    .setToken(firebaseNotificationRequest.firebaseToken())
                    .setNotification(
                            Notification
                                    .builder()
                                    .setTitle(firebaseNotificationRequest.title())
                                    .setBody(firebaseNotificationRequest.body())
                                    .build()
                    )
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);

            log.info("Successfully sent notification to device. Response: {}", response);

        } catch (Exception ex) {

            log.error("Error while sending notification to device. Exception {}", ex.getMessage(), ex);

            throw new RuntimeException("Failed to send notification to device", ex);
        }
    }
}
