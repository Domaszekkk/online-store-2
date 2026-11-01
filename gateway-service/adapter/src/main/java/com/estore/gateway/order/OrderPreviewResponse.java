package com.estore.gateway.order;

import com.estore.gateway.client.MoneyPayload;

import java.util.List;

public record OrderPreviewResponse(CustomerPayload customer,
                                   AddressPayload shippingAddress,
                                   BillingAddressPayload billingAddress,
                                   String paymentMethod,
                                   String deliveryMethod,
                                   List<OrderLineResponse> lines,
                                   MoneyPayload itemsTotal,
                                   MoneyPayload deliveryCost,
                                   MoneyPayload grandTotal,
                                   long cartVersion) {
}
