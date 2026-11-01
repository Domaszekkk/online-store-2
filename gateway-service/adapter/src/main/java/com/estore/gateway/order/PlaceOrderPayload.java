package com.estore.gateway.order;

import java.util.UUID;

public record PlaceOrderPayload(UUID cartId,
                                Long cartVersion,
                                CustomerPayload customer,
                                AddressPayload shippingAddress,
                                BillingAddressPayload billingAddress,
                                String paymentMethod,
                                String deliveryMethod) {
}
