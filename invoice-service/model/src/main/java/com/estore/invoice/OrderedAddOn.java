package com.estore.invoice;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderedAddOn {
    private String name;
    private Money price;
    private VatRate vatRate;
    private int quantity;
}
