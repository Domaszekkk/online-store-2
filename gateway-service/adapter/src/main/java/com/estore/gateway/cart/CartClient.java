package com.estore.gateway.cart;

import com.estore.gateway.client.RemoteServiceClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "cart-service", url = "${cart.service.url}", configuration = RemoteServiceClientConfiguration.class)
public interface CartClient {

    @PostMapping("/carts")
    CartResponse createCart();

    @GetMapping("/carts/{id}")
    CartResponse getCart(@PathVariable("id") UUID id);

    @PostMapping("/carts/{id}/items")
    CartResponse addItem(@PathVariable("id") UUID id, @RequestBody AddItemPayload payload);

    @PatchMapping("/carts/{id}/items/{itemId}")
    CartResponse changeItemQuantity(@PathVariable("id") UUID id,
                                    @PathVariable("itemId") UUID itemId,
                                    @RequestBody ChangeItemQuantityPayload payload);

    @DeleteMapping("/carts/{id}/items/{itemId}")
    CartResponse removeItem(@PathVariable("id") UUID id, @PathVariable("itemId") UUID itemId);
}
