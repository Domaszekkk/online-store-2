package com.estore.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddItemRequest {
    @NotNull
    private UUID productId;

    @NotNull
    private UUID variantId;

    @Positive
    private int quantity;

    @Valid
    @Builder.Default
    private List<@NotNull AddOnSelectionRequest> addOns = new ArrayList<>();
}
