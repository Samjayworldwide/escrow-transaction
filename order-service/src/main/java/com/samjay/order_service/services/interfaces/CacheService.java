package com.samjay.order_service.services.interfaces;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public interface CacheService {

    <T> Optional<T> get(String key, Class<T> type);

    <T> void set(String key, T value, Duration ttl);

    void delete(String key);

    <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader);
}
