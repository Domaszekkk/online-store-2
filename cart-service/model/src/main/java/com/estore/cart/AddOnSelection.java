package com.estore.cart;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddOnSelection {
    private UUID productId;
    private int quantity;
}
