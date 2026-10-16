package com.estore.order;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderLineDto {
    private UUID id;
    private UUID productId;
    private UUID variantId;
    private String productName;
    private String stockKeepingUnit;
    private MoneyDto unitPrice;
    private int quantity;
    private List<OrderLineAddOnDto> addOns;
    private MoneyDto lineTotal;
}
