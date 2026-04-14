package com.samjay.order_service.schedulers;

import com.samjay.order_service.entities.Order;
import com.samjay.order_service.enumerations.OrderStatus;
import com.samjay.order_service.repositories.OrderRepository;
import com.samjay.order_service.services.interfaces.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderScheduler {

    private final OrderRepository orderRepository;

    private final OrderService orderService;


    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void settleDeliveredOrders() {

        List<Order> orders = orderRepository.findAllOrdersToBeSettled(OrderStatus.DELIVERED, LocalDateTime.now().minusHours(1));

        log.info("Found {} orders to settle", orders.size());

        for (Order order : orders) {

            try {

                String clientRequestKey = "settle-order-" + order.getOrderReferenceNumber() + "-" + System.currentTimeMillis();

                orderService.settleOrder(clientRequestKey, order.getId());

                log.info("Settled order: {}", order.getOrderReferenceNumber());

            } catch (Exception e) {

                log.error("Failed to settle order {}: {}", order.getOrderReferenceNumber(), e.getMessage());

            }
        }
    }
}
