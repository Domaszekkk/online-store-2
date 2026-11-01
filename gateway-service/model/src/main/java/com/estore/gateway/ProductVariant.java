package com.estore.gateway;

import lombok.*;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductVariant {
    private UUID id;
    private Map<String, String> optionValues;
    private Money price;
    private String availability;
}
