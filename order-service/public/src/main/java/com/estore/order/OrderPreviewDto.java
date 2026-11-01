package com.estore.order;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderPreviewDto {
    private CustomerDto customer;
    private AddressDto shippingAddress;
    private BillingAddressDto billingAddress;
    private String paymentMethod;
    private String deliveryMethod;
    private List<OrderLineDto> lines;
    private MoneyDto itemsTotal;
    private MoneyDto deliveryCost;
    private MoneyDto grandTotal;
    private long cartVersion;
}
