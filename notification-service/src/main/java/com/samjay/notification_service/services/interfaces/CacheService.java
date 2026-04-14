package com.samjay.notification_service.services.interfaces;


import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.Optional;

public interface CacheService {

    <T> Optional<T> get(String key, Class<T> type);

    <T> Optional<T> get(String key, TypeReference<T> typeReference);

    <T> void set(String key, T value, Duration ttl);

    void delete(String key);
}
