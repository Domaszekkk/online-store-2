package com.estore.gateway;

import java.util.List;
import java.util.UUID;

public interface OrderPort {
    Order placeOrder(PlaceOrderCommand command);

    Order getOrder(UUID orderId);

    List<DeliveryOption> getDeliveryOptions();

    List<String> getPaymentMethods();
}
