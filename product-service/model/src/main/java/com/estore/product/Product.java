package com.estore.product;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        variants.forEach(variant -> variant.setId(UUID.randomUUID()));
        Product product = Product.builder()
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
        product.requireValidConfiguration();
        return product;
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
            assignVariantIds(command.getVariants());
            this.variants = new ArrayList<>(command.getVariants());
        }
        if (command.getAddOns() != null) {
            this.addOns = new ArrayList<>(command.getAddOns());
        }
        requireValidConfiguration();
    }

    public void archive(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public Set<String> stockKeepingUnits() {
        return variants.stream().map(ProductVariant::getStockKeepingUnit).collect(Collectors.toSet());
    }

    private void assignVariantIds(List<ProductVariant> newVariants) {
        Set<UUID> ownVariantIds = variants.stream().map(ProductVariant::getId).collect(Collectors.toSet());
        for (ProductVariant variant : newVariants) {
            if (variant.getId() == null) {
                variant.setId(UUID.randomUUID());
            } else if (!ownVariantIds.contains(variant.getId())) {
                throw InvalidProductException.unknownVariant(variant.getId(), id);
            }
        }
    }

    private void requireValidConfiguration() {
        if (variants.isEmpty()) {
            throw InvalidProductException.missingVariants();
        }
        requireUniqueStockKeepingUnits();
        if (type == ProductType.ELECTRONICS) {
            requireSimpleElectronics();
        } else {
            requireVariantsMatchingOptions();
        }
    }

    private void requireUniqueStockKeepingUnits() {
        Set<String> seen = new HashSet<>();
        for (ProductVariant variant : variants) {
            if (!seen.add(variant.getStockKeepingUnit())) {
                throw InvalidProductException.duplicateStockKeepingUnit(variant.getStockKeepingUnit());
            }
        }
    }

    private void requireSimpleElectronics() {
        if (!options.isEmpty()) {
            throw InvalidProductException.electronicsWithOptions();
        }
        if (variants.size() != 1) {
            throw InvalidProductException.electronicsWithManyVariants(variants.size());
        }
        if (!addOns.isEmpty()) {
            throw InvalidProductException.electronicsWithAddOns();
        }
        Map<String, String> optionValues = variants.get(0).getOptionValues();
        if (optionValues != null && !optionValues.isEmpty()) {
            throw InvalidProductException.electronicsWithOptions();
        }
    }

    private void requireVariantsMatchingOptions() {
        Map<String, List<String>> valuesPerOption = new HashMap<>();
        for (ProductOption option : options) {
            if (valuesPerOption.put(option.getName(), option.getValues()) != null) {
                throw InvalidProductException.duplicateOptionName(option.getName());
            }
        }
        Set<Map<String, String>> combinations = new HashSet<>();
        for (ProductVariant variant : variants) {
            Map<String, String> optionValues = variant.getOptionValues() != null ? variant.getOptionValues() : Map.of();
            if (!optionValues.keySet().equals(valuesPerOption.keySet())) {
                throw InvalidProductException.variantOptionsMismatch(variant.getStockKeepingUnit(), valuesPerOption.keySet());
            }
            optionValues.forEach((optionName, value) -> {
                if (!valuesPerOption.get(optionName).contains(value)) {
                    throw InvalidProductException.unknownOptionValue(variant.getStockKeepingUnit(), optionName, value);
                }
            });
            if (!combinations.add(optionValues)) {
                throw InvalidProductException.duplicateOptionCombination(variant.getStockKeepingUnit());
            }
        }
    }
}
