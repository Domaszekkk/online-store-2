package com.estore.gateway.order;

import com.estore.gateway.client.MoneyPayload;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id,
                            String orderNumber,
                            String status,
                            CustomerPayload customer,
                            AddressPayload shippingAddress,
                            BillingAddressPayload billingAddress,
                            String paymentMethod,
                            String deliveryMethod,
                            List<OrderLineResponse> lines,
                            MoneyPayload itemsTotal,
                            MoneyPayload deliveryCost,
                            MoneyPayload grandTotal,
                            String documentNumber,
                            Instant createdAt) {
}
