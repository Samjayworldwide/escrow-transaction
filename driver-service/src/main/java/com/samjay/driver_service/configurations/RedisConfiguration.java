package com.samjay.driver_service.configurations;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.*;
import redis.clients.jedis.providers.PooledConnectionProvider;

import java.time.Duration;

@Configuration
public class RedisConfiguration {

    @Value("${redis.pool.max-total:20}")
    public int maxTotal;

    @Value("${redis.pool.max-idle:10}")
    public int maxIdle;

    @Value("${redis.pool.min-idle:5}")
    public int minIdle;

    @Value("${redis.pool.max-wait-millis:3000}")
    public long maxWaitMillis;

    @Value("${redis.host:localhost}")
    public String redisHost;

    @Value("${redis.port:6379}")
    public int port;

    @Bean
    public UnifiedJedis unifiedJedis() {

        HostAndPort hostAndPort = new HostAndPort(redisHost, port);

        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(2000)
                .socketTimeoutMillis(2000)
                .build();

        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();

        poolConfig.setMaxTotal(maxTotal);

        poolConfig.setMaxIdle(maxIdle);

        poolConfig.setMinIdle(minIdle);

        poolConfig.setMaxWait(Duration.ofMillis(maxWaitMillis));

        poolConfig.setTestOnBorrow(true);

        poolConfig.setTestWhileIdle(true);

        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));

        PooledConnectionProvider provider = new PooledConnectionProvider(
                hostAndPort,
                clientConfig,
                poolConfig
        );

        return new UnifiedJedis(provider);
    }
}

