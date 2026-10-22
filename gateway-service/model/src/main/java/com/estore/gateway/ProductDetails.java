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
public class ProductDetails {
    private UUID id;
    private String name;
    private String type;
    private String description;
    private Map<String, String> attributes;
    private List<ProductOption> options;
    private List<ProductVariant> variants;
    private List<ProductAddOn> addOns;
}
