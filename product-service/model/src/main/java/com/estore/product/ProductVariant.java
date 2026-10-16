package com.estore.product;

import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductVariant {
    private UUID id;
    private String stockKeepingUnit;

    @Builder.Default
    private Map<String, String> optionValues = new HashMap<>();
    private Money price;
    private Availability availability;
}
