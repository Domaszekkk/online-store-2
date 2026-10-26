package com.estore.gateway;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderLineDto {
    private String productName;
    private MoneyDto unitPrice;
    private int quantity;
    private List<OrderLineAddOnDto> addOns;
    private MoneyDto lineTotal;
}
