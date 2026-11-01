package com.estore.gateway;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlaceOrderRequest {
    @NotNull
    private UUID cartId;
    @NotNull(groups = OrderPlacement.class)
    private Long cartVersion;
    @NotNull
    private CustomerRequest customer;
    @NotNull
    private AddressRequest shippingAddress;
    private BillingAddressRequest billingAddress;
    @NotNull
    private String paymentMethod;
    @NotNull
    private String deliveryMethod;
}
