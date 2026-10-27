package com.estore.gateway;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderPreview {
    private Customer customer;
    private Address shippingAddress;
    private BillingAddress billingAddress;
    private String paymentMethod;
    private String deliveryMethod;
    private List<OrderLine> lines;
    private Money itemsTotal;
    private Money deliveryCost;
    private Money grandTotal;
}
