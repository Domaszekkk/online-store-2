package com.estore.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangeItemQuantityRequest {
    @Positive
    @Max(99)
    private int quantity;
}
