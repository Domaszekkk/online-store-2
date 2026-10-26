package com.estore.gateway;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDetailsDto {
    private UUID id;
    private String name;
    private String type;
    private String description;
    private Map<String, String> attributes;
    private List<ProductOptionDto> options;
    private List<ProductVariantDto> variants;
    private List<ProductAddOnDto> addOns;
}
