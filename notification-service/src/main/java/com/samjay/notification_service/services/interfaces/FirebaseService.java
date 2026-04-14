package com.samjay.notification_service.services.interfaces;

import com.samjay.notification_service.dtos.requests.FirebaseNotificationRequest;

public interface FirebaseService {

    void sendNotificationToDevice(FirebaseNotificationRequest firebaseNotificationRequest);

}
