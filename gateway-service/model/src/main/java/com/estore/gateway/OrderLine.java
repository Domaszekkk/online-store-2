package com.estore.gateway;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderLine {
    private String productName;
    private Money unitPrice;
    private int quantity;
    private List<OrderLineAddOn> addOns;
    private Money lineTotal;
}
