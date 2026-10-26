package com.estore.product;

import lombok.*;

import java.time.Instant;
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

    private Instant archivedAt;

    public static Product from(AddProductCommand command) {
        List<ProductVariant> variants = command.getVariants() != null ? command.getVariants() : new ArrayList<>();
        assignMissingVariantIds(variants);
        return Product.builder()
                .id(UUID.randomUUID())
                .name(command.getName())
                .type(command.getType())
                .description(command.getDescription())
                .basePrice(command.getBasePrice())
                .attributes(command.getAttributes() != null ? command.getAttributes() : new HashMap<>())
                .options(command.getOptions() != null ? command.getOptions() : new ArrayList<>())
                .variants(variants)
                .addOns(command.getAddOns() != null ? command.getAddOns() : new ArrayList<>())
                .build();
    }

    public void update(UpdateProductCommand command) {
        this.name = command.getName();
        this.type = command.getType();
        this.description = command.getDescription();
        this.basePrice = command.getBasePrice();
        if (command.getAttributes() != null) {
            this.attributes = command.getAttributes();
        }
        if (command.getOptions() != null) {
            this.options = new ArrayList<>(command.getOptions());
        }
        if (command.getVariants() != null) {
            assignMissingVariantIds(command.getVariants());
            this.variants = new ArrayList<>(command.getVariants());
        }
        if (command.getAddOns() != null) {
            this.addOns = new ArrayList<>(command.getAddOns());
        }
    }

    public void archive(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    private static void assignMissingVariantIds(List<ProductVariant> variants) {
        variants.forEach(variant -> {
            if (variant.getId() == null) {
                variant.setId(UUID.randomUUID());
            }
        });
    }
}
