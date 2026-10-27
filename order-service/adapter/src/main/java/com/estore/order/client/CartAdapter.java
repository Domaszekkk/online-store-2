package com.estore.order.client;

import com.estore.order.CartPort;
import com.estore.order.CartDetails;
import com.estore.order.CartItemAddOnDetails;
import com.estore.order.CartItemDetails;
import com.estore.order.CartNotFoundException;
import com.estore.order.CartNotOrderableException;
import com.estore.order.CartServiceUnavailableException;
import com.estore.order.Money;
import com.estore.order.VatRate;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartAdapter implements CartPort {
    private final CartClient cartClient;
    private final JsonMapper jsonMapper;

    @Override
    public CartDetails checkout(UUID cartId) {
        try {
            return map(cartClient.checkout(cartId));
        } catch (FeignException.NotFound exception) {
            throw new CartNotFoundException(cartId);
        } catch (FeignException.Conflict exception) {
            throw new CartNotOrderableException(cartId, reasonFrom(exception));
        } catch (FeignException exception) {
            log.warn("Cart service checkout failed for cart {} (status {})", cartId, exception.status(), exception);
            throw new CartServiceUnavailableException();
        }
    }

    @Override
    public CartDetails getCart(UUID cartId) {
        try {
            return map(cartClient.getCart(cartId));
        } catch (FeignException.NotFound exception) {
            throw new CartNotFoundException(cartId);
        } catch (FeignException exception) {
            log.warn("Cart service call failed for cart {} (status {})", cartId, exception.status(), exception);
            throw new CartServiceUnavailableException();
        }
    }

    private String reasonFrom(FeignException exception) {
        try {
            ErrorResponse error = jsonMapper.readValue(exception.contentUTF8(), ErrorResponse.class);
            if (error != null && error.message() != null) {
                return error.message();
            }
        } catch (JacksonException ignored) {
        }
        return "cart is empty or already checked out";
    }

    private CartDetails map(CartResponse response) {
        List<CartItemDetails> items = (response.items() == null ? List.<CartItemResponse>of() : response.items())
                .stream()
                .map(item -> CartItemDetails.builder()
                        .productId(item.productId())
                        .variantId(item.variantId())
                        .productName(item.productName())
                        .stockKeepingUnit(item.stockKeepingUnit())
                        .unitPrice(money(item.unitPrice()))
                        .vatRate(vatRate(item.vatRate()))
                        .quantity(item.quantity())
                        .addOns((item.addOns() == null ? List.<CartItemAddOnResponse>of() : item.addOns())
                                .stream()
                                .map(addOn -> CartItemAddOnDetails.builder()
                                        .productId(addOn.productId())
                                        .name(addOn.name())
                                        .price(money(addOn.price()))
                                        .vatRate(vatRate(addOn.vatRate()))
                                        .quantity(addOn.quantity())
                                        .build())
                                .toList())
                        .build())
                .toList();
        return CartDetails.builder()
                .id(response.id())
                .items(items)
                .build();
    }

    private VatRate vatRate(String value) {
        if (value == null) {
            throw new IllegalStateException("Cart service returned a price without a VAT rate");
        }
        return VatRate.valueOf(value);
    }

    private Money money(MoneyPayload payload) {
        return payload == null ? null : Money.of(payload.amount(), payload.currency());
    }
}
