package com.estore.invoice;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VatSummary {
    private VatRate vatRate;
    private Money netTotal;
    private Money vatTotal;
    private Money grossTotal;

    public static VatSummary fromGross(VatRate vatRate, Money grossTotal) {
        Money netTotal = vatRate.netFromGross(grossTotal);
        return VatSummary.builder()
                .vatRate(vatRate)
                .netTotal(netTotal)
                .vatTotal(grossTotal.subtract(netTotal))
                .grossTotal(grossTotal)
                .build();
    }
}
