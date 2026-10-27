package com.estore.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@FeignClient(name = "cart-service", url = "${cart.service.url}", configuration = CartClientConfiguration.class)
public interface CartClient {

    @PostMapping("/carts/{id}/checkout")
    CartResponse checkout(@PathVariable("id") UUID id);

    @GetMapping("/carts/{id}")
    CartResponse getCart(@PathVariable("id") UUID id);
}
