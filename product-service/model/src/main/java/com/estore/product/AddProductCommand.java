package com.estore.product;

import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddProductCommand {
    private String name;
    private ProductType type;
    private String description;
    private Money basePrice;

    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();

    @Builder.Default
    private List<ProductOption> options = new ArrayList<>();

    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @Builder.Default
    private List<AddOn> addOns = new ArrayList<>();
}
