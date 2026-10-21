package com.estore.order;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderLineAddOn {
    private UUID productId;
    private String name;
    private Money price;
    private VatRate vatRate;
    private int quantity;

    public Money total() {
        return price.multiply(quantity);
    }
}
