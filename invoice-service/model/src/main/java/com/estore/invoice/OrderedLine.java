package com.estore.invoice;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderedLine {
    private String productName;
    private String stockKeepingUnit;
    private Money unitPrice;
    private VatRate vatRate;
    private int quantity;

    @Builder.Default
    private List<OrderedAddOn> addOns = new ArrayList<>();
}
