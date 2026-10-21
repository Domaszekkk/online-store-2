package com.estore.cart;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangeItemQuantityCommand {
    private int quantity;
}
