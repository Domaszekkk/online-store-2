package com.estore.gateway.order;

import com.estore.gateway.DeliveryOption;
import com.estore.gateway.Order;
import com.estore.gateway.OrderPort;
import com.estore.gateway.PlaceOrderCommand;
import com.estore.gateway.client.RemoteServiceCalls;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderAdapter implements OrderPort {
    private static final String SERVICE_NAME = "order-service";

    private final OrderClient orderClient;
    private final OrderMapper orderMapper;
    private final RemoteServiceCalls remoteServiceCalls;

    @Override
    public Order placeOrder(PlaceOrderCommand command) {
        PlaceOrderPayload payload = orderMapper.toPayload(command);
        return orderMapper.toModel(remoteServiceCalls.call(SERVICE_NAME, () -> orderClient.placeOrder(payload)));
    }

    @Override
    public Order getOrder(UUID orderId) {
        return orderMapper.toModel(remoteServiceCalls.call(SERVICE_NAME, () -> orderClient.getOrder(orderId)));
    }

    @Override
    public List<DeliveryOption> getDeliveryOptions() {
        return orderMapper.toDeliveryOptions(remoteServiceCalls.call(SERVICE_NAME, orderClient::getDeliveryMethods));
    }

    @Override
    public List<String> getPaymentMethods() {
        return remoteServiceCalls.call(SERVICE_NAME, orderClient::getPaymentMethods);
    }
}
