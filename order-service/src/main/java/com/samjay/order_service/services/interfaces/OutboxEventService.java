package com.samjay.order_service.services.interfaces;


public interface OutboxEventService {

    void saveEvent(String aggregateId, String eventType, String kafkaBinding, Object payload, String clientRequestKey);
}
