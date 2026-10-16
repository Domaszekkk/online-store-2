package com.estore.order;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlaceOrderCommand {
    private UUID cartId;
    private Customer customer;
    private Address shippingAddress;
    private Address billingAddress;
    private PaymentMethod paymentMethod;
    private DeliveryMethod deliveryMethod;
}
