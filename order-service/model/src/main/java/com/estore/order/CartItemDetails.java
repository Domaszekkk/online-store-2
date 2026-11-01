package com.estore.order;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItemDetails {
    private UUID productId;
    private UUID variantId;
    private String productName;
    private String stockKeepingUnit;
    private Money unitPrice;
    private VatRate vatRate;
    private int quantity;

    @Builder.Default
    private List<CartItemAddOnDetails> addOns = new ArrayList<>();

    @Builder.Default
    private boolean available = true;
}
