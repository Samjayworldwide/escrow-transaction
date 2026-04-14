package com.samjay.notification_service.services.implementations;

import com.samjay.notification_service.configurations.AuthenticatedUserProvider;
import com.samjay.notification_service.dtos.requests.FetchNotificationRequest;
import com.samjay.notification_service.dtos.requests.FirebaseNotificationRequest;
import com.samjay.notification_service.dtos.responses.ApiResponse;
import com.samjay.notification_service.dtos.responses.CursorPaginatedResponse;
import com.samjay.notification_service.dtos.responses.NotificationResponse;
import com.samjay.notification_service.dtos.responses.UserIdentifier;
import com.samjay.notification_service.entities.Device;
import com.samjay.notification_service.entities.Notification;
import com.samjay.notification_service.models.CursorPayload;
import com.samjay.notification_service.repositories.DeviceRepository;
import com.samjay.notification_service.repositories.NotificationRepository;
import com.samjay.notification_service.services.interfaces.CacheService;
import com.samjay.notification_service.services.interfaces.FirebaseService;
import com.samjay.notification_service.services.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.samjay.notification_service.utilities.AppExtensions.decodeCursor;
import static com.samjay.notification_service.utilities.AppExtensions.encodeCursor;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImplementation implements NotificationService {

    private final DeviceRepository deviceRepository;

    private final FirebaseService firebaseService;

    private final NotificationRepository notificationRepository;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    private final CacheService cacheService;

    @Override
    public void sendAndSavePushNotification(UUID userId, String title, String body, String idempotencyKey) {

        try {

            List<Device> devices = deviceRepository.findByUserId(userId);

            if (devices.isEmpty()) {

                log.warn("No devices found for user with ID: {}", userId);

                return;

            }

            boolean exists = notificationRepository.existsByUserIdAndIdempotencyKey(userId, idempotencyKey);

            if (exists) {

                log.warn("Notification with idempotency key: {} already exists for user with ID: {}. Skipping notification.", idempotencyKey, userId);

                return;

            }

            saveNotification(userId, title, body, idempotencyKey);

            String firstPageCacheKey = buildCacheKey(userId.toString(), null);

            cacheService.delete(firstPageCacheKey);

            log.info("Invalidated first page cache for user: {}", userId);

            for (Device device : devices) {

                log.info("Sending notification to device with token: {}", device.getFirebaseToken());

                FirebaseNotificationRequest firebaseDeviceNotificationRequest = new FirebaseNotificationRequest(
                        device.getFirebaseToken(),
                        title,
                        body
                );

                firebaseService.sendNotificationToDevice(firebaseDeviceNotificationRequest);

                log.info("Notification sent to device with token: {}", device.getFirebaseToken());

            }

        } catch (Exception ex) {

            log.error("Error while sending push notification to user with ID: {}. Exception: {}", userId, ex.getMessage(), ex);

            throw new RuntimeException(ex.getMessage(), ex);

        }
    }

    @Override
    public ApiResponse<CursorPaginatedResponse<NotificationResponse>> fetchUserNotifications(FetchNotificationRequest fetchNotificationRequest) {

        UserIdentifier userIdentifier = authenticatedUserProvider.getCurrentLoggedInUser();

        String cacheKey = buildCacheKey(userIdentifier.userId(), fetchNotificationRequest.getCursor());

        Optional<CursorPaginatedResponse<NotificationResponse>> cached = cacheService.get(
                cacheKey,
                new TypeReference<>() {
                }
        );

        if (cached.isPresent()) {

            log.info("Cache hit for key: {}", cacheKey);

            return ApiResponse.success("Notifications fetched successfully", cached.get());

        }

        log.info("Cache miss for key: {}. Fetching notifications from database.", cacheKey);

        CursorPayload cursor = decodeCursor(fetchNotificationRequest.getCursor());

        // Fetch pageSize + 1 to determine if there are more pages
        Pageable pageable = PageRequest.of(0, fetchNotificationRequest.getPageSize() + 1);

        List<Notification> notifications;

        if (cursor == null) {

            notifications = notificationRepository.findNotificationsFirstPage(
                    UUID.fromString(userIdentifier.userId()),
                    pageable
            );

        } else {

            notifications = notificationRepository.findNotificationsAfterCursor(
                    UUID.fromString(userIdentifier.userId()),
                    cursor.getLastCreatedAt(),
                    cursor.getLastId(),
                    pageable
            );

        }

        // Check if there are more records beyond this page
        boolean hasMore = notifications.size() > fetchNotificationRequest.getPageSize();

        if (hasMore)
            notifications = notifications.subList(0, fetchNotificationRequest.getPageSize());

        // Build next cursor from the last item in the current page
        String nextCursor = null;

        if (hasMore) {

            Notification last = notifications.getLast();

            nextCursor = encodeCursor(new CursorPayload(
                    last.getCreatedAt(),
                    last.getId()
            ));

        }

        // Map entities to response DTOs
        List<NotificationResponse> responseItems = notifications
                .stream()
                .map(t -> new NotificationResponse(
                        t.getTitle(),
                        t.getMessage(),
                        t.isRead(),
                        t.getCreatedAt()
                ))
                .toList();

        CursorPaginatedResponse<NotificationResponse> paginatedResponse =
                new CursorPaginatedResponse<>(
                        responseItems,
                        nextCursor,
                        hasMore,
                        fetchNotificationRequest.getPageSize()
                );

        Duration ttl = (fetchNotificationRequest.getCursor() == null || fetchNotificationRequest.getCursor().isBlank())
                ? Duration.ofMinutes(2)
                : Duration.ofMinutes(10);

        cacheService.set(cacheKey, paginatedResponse, ttl);

        return ApiResponse.success("Notifications fetched successfully", paginatedResponse);
    }

    private void saveNotification(UUID userId, String title, String message, String idempotencyKey) {

        Notification notification = Notification
                .builder()
                .userId(userId)
                .title(title)
                .message(message)
                .idempotencyKey(idempotencyKey)
                .build();

        notificationRepository.save(notification);

    }

    private String buildCacheKey(String userId, String cursor) {

        String cursorPart = (cursor == null || cursor.isBlank()) ? "first" : cursor;

        return "notifications:" + userId + ":" + cursorPart;
    }
}
