package com.estore.gateway;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItemDto {
    private UUID id;
    private UUID productId;
    private UUID variantId;
    private String productName;
    private MoneyDto unitPrice;
    private int quantity;
    private List<CartItemAddOnDto> addOns;
    private MoneyDto lineTotal;
    private boolean available;
}
