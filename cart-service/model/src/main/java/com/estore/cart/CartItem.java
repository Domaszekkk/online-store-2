package com.estore.cart;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItem {
    private UUID id;
    private UUID productId;
    private UUID variantId;
    private String productName;
    private String stockKeepingUnit;
    private Money unitPrice;
    private int quantity;

    @Builder.Default
    private List<CartItemAddOn> addOns = new ArrayList<>();

    private Money lineTotal;

    public static CartItem create(UUID productId,
                                  String productName,
                                  VariantDetails variant,
                                  int quantity,
                                  List<CartItemAddOn> addOns) {
        requireValidQuantity(quantity);
        CartItem item = CartItem.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .variantId(variant.getId())
                .productName(productName)
                .stockKeepingUnit(variant.getStockKeepingUnit())
                .unitPrice(variant.getPrice())
                .quantity(quantity)
                .addOns(addOns)
                .build();
        item.recomputeLineTotal();
        return item;
    }

    public static void requireValidQuantity(int quantity) {
        if (quantity < 1) {
            throw new InvalidQuantityException(quantity);
        }
    }

    public void changeQuantity(int quantity) {
        requireValidQuantity(quantity);
        this.quantity = quantity;
        recomputeLineTotal();
    }

    public boolean hasSameConfigurationAs(CartItem other) {
        return variantId.equals(other.variantId) && addOnProductIds().equals(other.addOnProductIds());
    }

    public void absorb(CartItem other) {
        this.quantity += other.quantity;
        for (CartItemAddOn otherAddOn : other.addOns) {
            addOns.stream()
                    .filter(addOn -> addOn.getProductId().equals(otherAddOn.getProductId()))
                    .findFirst()
                    .ifPresent(addOn -> addOn.setQuantity(addOn.getQuantity() + otherAddOn.getQuantity()));
        }
        recomputeLineTotal();
    }

    private Set<UUID> addOnProductIds() {
        if (addOns == null) {
            return Set.of();
        }
        return addOns.stream().map(CartItemAddOn::getProductId).collect(Collectors.toSet());
    }

    private void recomputeLineTotal() {
        Money addOnsTotal = (addOns == null ? List.<CartItemAddOn>of() : addOns).stream()
                .map(CartItemAddOn::total)
                .reduce(Money.zero(unitPrice.currency()), Money::add);
        this.lineTotal = unitPrice.multiply(quantity).add(addOnsTotal);
    }
}
