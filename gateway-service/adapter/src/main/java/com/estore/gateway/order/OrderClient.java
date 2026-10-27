package com.estore.gateway.order;

import com.estore.gateway.client.RemoteServiceClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "order-service", url = "${order.service.url}", configuration = RemoteServiceClientConfiguration.class)
public interface OrderClient {

    @PostMapping("/orders/preview")
    OrderPreviewResponse previewOrder(@RequestBody PlaceOrderPayload payload);

    @PostMapping("/orders")
    OrderResponse placeOrder(@RequestBody PlaceOrderPayload payload);

    @GetMapping("/orders/{id}")
    OrderResponse getOrder(@PathVariable("id") UUID id);

    @GetMapping("/orders/delivery-methods")
    List<DeliveryMethodResponse> getDeliveryMethods();

    @GetMapping("/orders/payment-methods")
    List<String> getPaymentMethods();
}
