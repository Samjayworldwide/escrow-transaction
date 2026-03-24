package com.samjay.order_service.configurations;

import com.samjay.CustomerServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcConfiguration {

    @Bean
    public CustomerServiceGrpc.CustomerServiceBlockingStub getCustomerServiceBlockingStub(GrpcChannelFactory channels) {

        return CustomerServiceGrpc.newBlockingStub(channels.createChannel("customer-service"));
    }
}
