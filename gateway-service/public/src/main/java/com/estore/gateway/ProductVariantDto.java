package com.estore.gateway;

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
    private Map<String, String> optionValues;
    private MoneyDto price;
    private String availability;
}
