package com.estore.gateway;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItem {
    private UUID id;
    private UUID productId;
    private UUID variantId;
    private String productName;
    private Money unitPrice;
    private int quantity;
    private List<CartItemAddOn> addOns;
    private Money lineTotal;
    private boolean available;
}
