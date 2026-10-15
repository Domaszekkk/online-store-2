package com.estore.cart;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItemAddOnDto {
    private UUID productId;
    private String name;
    private MoneyDto price;
    private int quantity;
}
