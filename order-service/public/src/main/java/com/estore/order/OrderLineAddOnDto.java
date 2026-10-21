package com.estore.order;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderLineAddOnDto {
    private UUID productId;
    private String name;
    private MoneyDto price;
    private String vatRate;
    private int quantity;
}
