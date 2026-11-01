package com.estore.gateway;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderLineAddOn {
    private String name;
    private Money price;
    private int quantity;
}
