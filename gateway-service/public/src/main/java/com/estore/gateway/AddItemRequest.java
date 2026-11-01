package com.estore.gateway;

import jakarta.validation.constraints.NotNull;
import lombok.*;

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
    private int quantity;
    private List<AddOnSelectionRequest> addOns;
}
