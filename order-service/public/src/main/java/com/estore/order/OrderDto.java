package com.estore.order;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDto {
    private UUID id;
    private String orderNumber;
    private UUID cartId;
    private String status;
    private CustomerDto customer;
    private AddressDto shippingAddress;
    private BillingAddressDto billingAddress;
    private String paymentMethod;
    private String deliveryMethod;
    private List<OrderLineDto> lines;
    private MoneyDto itemsTotal;
    private MoneyDto deliveryCost;
    private MoneyDto grandTotal;
    private String documentNumber;
    private Instant createdAt;
}
