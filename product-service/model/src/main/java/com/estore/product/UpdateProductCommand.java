package com.estore.product;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Full product update. Collections left as {@code null} keep their current values;
 * a provided collection (also an empty one) replaces the existing one.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateProductCommand {
    private String name;
    private ProductType type;
    private String description;
    private Money basePrice;

    private Map<String, String> attributes;

    private List<ProductOption> options;
    private List<ProductVariant> variants;
    private List<AddOn> addOns;
}
