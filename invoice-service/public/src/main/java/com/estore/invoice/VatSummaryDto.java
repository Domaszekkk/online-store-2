package com.estore.invoice;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VatSummaryDto {
    private String vatRate;
    private MoneyDto netTotal;
    private MoneyDto vatTotal;
    private MoneyDto grossTotal;
}
