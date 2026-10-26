package com.estore.invoice;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentLineDto {
    private int position;
    private String name;
    private String stockKeepingUnit;
    private int quantity;
    private MoneyDto unitGrossPrice;
    private String vatRate;
    private MoneyDto netTotal;
    private MoneyDto vatTotal;
    private MoneyDto grossTotal;
}
