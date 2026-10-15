package com.estore.cart;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddItemCommand {
    private UUID productId;
    private UUID variantId;
    private int quantity;

    @Builder.Default
    private List<AddOnSelection> addOns = new ArrayList<>();
}
