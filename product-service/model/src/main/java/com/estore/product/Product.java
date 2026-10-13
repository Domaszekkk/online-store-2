package com.estore.product;

import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Product {
    private UUID id;
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

    public static Product from(AddProductCommand command) {
        List<ProductVariant> variants = command.getVariants();
        variants.forEach(variant -> {
            if (variant.getId() == null) {
                variant.setId(UUID.randomUUID());
            }
        });
        return Product.builder()
                .id(UUID.randomUUID())
                .name(command.getName())
                .type(command.getType())
                .description(command.getDescription())
                .basePrice(command.getBasePrice())
                .attributes(command.getAttributes())
                .options(command.getOptions())
                .variants(variants)
                .addOns(command.getAddOns())
                .build();
    }

    public void update(UpdateProductRequest request) {
        this.name = request.getName();
        this.type = request.getType();
        this.description = request.getDescription();
        this.basePrice = request.getBasePrice();
        if (request.getAttributes() != null) {
            this.attributes = request.getAttributes();
        }
    }
}
