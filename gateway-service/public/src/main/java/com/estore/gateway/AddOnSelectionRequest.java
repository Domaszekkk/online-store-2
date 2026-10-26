package com.estore.gateway;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddOnSelectionRequest {
    private UUID productId;
    private int quantity;
}
