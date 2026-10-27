package com.estore.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddOnSelectionRequest {
    @NotNull
    private UUID productId;

    @Positive
    @Max(99)
    private int quantity;
}
