package com.estore.gateway;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Order {
    private UUID id;
    private String orderNumber;
    private String status;
    private Customer customer;
    private Address shippingAddress;
    private BillingAddress billingAddress;
    private String paymentMethod;
    private String deliveryMethod;
    private List<OrderLine> lines;
    private Money itemsTotal;
    private Money deliveryCost;
    private Money grandTotal;
    private Instant createdAt;
}
