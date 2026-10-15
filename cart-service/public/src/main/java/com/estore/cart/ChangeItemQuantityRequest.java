package com.estore.cart;

import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangeItemQuantityRequest {
    @Positive
    private int quantity;
}
