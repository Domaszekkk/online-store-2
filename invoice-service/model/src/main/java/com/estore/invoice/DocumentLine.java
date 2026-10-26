package com.estore.invoice;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentLine {
    private int position;
    private String name;
    private String stockKeepingUnit;
    private int quantity;
    private Money unitGrossPrice;
    private VatRate vatRate;
    private Money netTotal;
    private Money vatTotal;
    private Money grossTotal;

    public static DocumentLine of(int position,
                                  String name,
                                  String stockKeepingUnit,
                                  int quantity,
                                  Money unitGrossPrice,
                                  VatRate vatRate) {
        Money grossTotal = unitGrossPrice.multiply(quantity);
        Money netTotal = vatRate.netFromGross(grossTotal);
        return DocumentLine.builder()
                .position(position)
                .name(name)
                .stockKeepingUnit(stockKeepingUnit)
                .quantity(quantity)
                .unitGrossPrice(unitGrossPrice)
                .vatRate(vatRate)
                .netTotal(netTotal)
                .vatTotal(grossTotal.subtract(netTotal))
                .grossTotal(grossTotal)
                .build();
    }
}
