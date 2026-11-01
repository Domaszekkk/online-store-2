package com.estore.gateway;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlaceOrderCommand {
    private UUID cartId;
    private Long cartVersion;
    private Customer customer;
    private Address shippingAddress;
    private BillingAddress billingAddress;
    private String paymentMethod;
    private String deliveryMethod;
}
