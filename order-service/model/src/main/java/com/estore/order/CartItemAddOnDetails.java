package com.estore.order;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItemAddOnDetails {
    private UUID productId;
    private String name;
    private Money price;
    private int quantity;
}
