package com.estore.product;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDto {
    private UUID id;
    private String name;
    private String type;
    private String description;
    private MoneyDto basePrice;
    private Map<String, String> attributes;
    private List<ProductOptionDto> options;
    private List<ProductVariantDto> variants;
    private List<AddOnDto> addOns;
    private Instant archivedAt;
}
