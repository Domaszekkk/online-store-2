package com.estore.gateway;

import java.util.List;
import java.util.UUID;

public interface OrderPort {
    OrderPreview previewOrder(PlaceOrderCommand command);

    Order placeOrder(PlaceOrderCommand command);

    Order getOrder(UUID orderId);

    List<DeliveryOption> getDeliveryOptions();

    List<String> getPaymentMethods();
}
