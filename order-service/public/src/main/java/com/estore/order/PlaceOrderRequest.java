package com.estore.order;

import jakarta.validation.Valid;
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
    @Valid
    private CustomerRequest customer;

    @NotNull
    @Valid
    private AddressRequest shippingAddress;

    @Valid
    private BillingAddressRequest billingAddress;

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull
    private DeliveryMethod deliveryMethod;
}
