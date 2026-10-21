package com.estore.product;

import lombok.*;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductVariantDto {
    private UUID id;
    private String stockKeepingUnit;
    private Map<String, String> optionValues;
    private MoneyDto price;
    private String availability;
    private String vatRate;
}
