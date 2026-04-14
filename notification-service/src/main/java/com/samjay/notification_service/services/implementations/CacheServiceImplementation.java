package com.samjay.notification_service.services.implementations;

import com.samjay.notification_service.services.interfaces.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@SuppressWarnings("ALL")
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheServiceImplementation implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper redisObjectMapper;

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {

        try {

            Object value = redisTemplate.opsForValue().get(key);

            if (value == null) return Optional.empty();

            if (type.isInstance(value))
                return Optional.of(type.cast(value));

        } catch (Exception ex) {

            log.warn("Redis GET failed for key {}", key, ex);

        }

        return Optional.empty();
    }

    @Override
    public <T> Optional<T> get(String key, TypeReference<T> typeReference) {

        try {

            Object value = redisTemplate.opsForValue().get(key);

            if (value == null) return Optional.empty();

            T result = redisObjectMapper.convertValue(value, typeReference);

            return Optional.of(result);

        } catch (Exception ex) {

            log.warn("Redis GET (TypeReference) failed for key {}", key, ex);

        }

        return Optional.empty();

    }

    @Override
    public <T> void set(String key, T value, Duration ttl) {

        try {

            redisTemplate.opsForValue().set(key, value, ttl);

        } catch (Exception ex) {

            log.warn("Redis SET failed for key {}", key, ex);

        }
    }

    @Override
    public void delete(String key) {

        try {

            redisTemplate.delete(key);

        } catch (Exception ex) {

            log.warn("Redis DELETE failed for key {}", key, ex);

        }
    }
}
