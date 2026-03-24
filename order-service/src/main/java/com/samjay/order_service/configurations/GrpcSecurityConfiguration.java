package com.samjay.order_service.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;

@Configuration
public class GrpcSecurityConfiguration {

    @Bean
    public AuthenticationProcessInterceptor grpcSecurityInterceptor(GrpcSecurity grpc) throws Exception {
        return grpc
                .authorizeRequests(requests -> requests
                        .allRequests().permitAll()
                )
                .build();
    }
}