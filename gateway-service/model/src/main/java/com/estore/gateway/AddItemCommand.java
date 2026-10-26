package com.estore.gateway;

import lombok.*;

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
    private List<AddOnSelection> addOns;
}
