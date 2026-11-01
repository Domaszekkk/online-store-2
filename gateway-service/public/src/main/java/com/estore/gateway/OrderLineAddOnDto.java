package com.estore.gateway;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderLineAddOnDto {
    private String name;
    private MoneyDto price;
    private int quantity;
}
