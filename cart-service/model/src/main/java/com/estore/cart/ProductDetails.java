package com.estore.cart;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDetails {
    private UUID id;
    private String name;

    @Builder.Default
    private List<VariantDetails> variants = new ArrayList<>();

    @Builder.Default
    private List<AddOnDetails> addOns = new ArrayList<>();

    public Optional<VariantDetails> findVariant(UUID variantId) {
        return variants.stream().filter(variant -> variant.getId().equals(variantId)).findFirst();
    }

    public Optional<AddOnDetails> findAddOn(UUID productId) {
        return addOns.stream().filter(addOn -> addOn.getProductId().equals(productId)).findFirst();
    }
}
