package com.samjay.payment_service.configuration;

import com.samjay.OrderServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcConfiguration {

    @Bean
    public OrderServiceGrpc.OrderServiceBlockingStub getOrderServiceBlockingStub(GrpcChannelFactory channels) {

        return OrderServiceGrpc.newBlockingStub(channels.createChannel("order-service"));

    }
}
