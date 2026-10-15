package com.estore.cart;

import lombok.*;

import java.util.UUID;

/**
 * Add-on chosen for a cart item, with its own quantity independent of the item quantity
 * (e.g. two laptops but only one cable).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddOnSelection {
    private UUID productId;
    private int quantity;
}
