package com.estore.cart;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItemAddOn {
    private UUID productId;
    private String name;
    private Money price;
    private int quantity;

    public Money total() {
        return price.multiply(quantity);
    }
}
