package com.estore.cart;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItem {
    public static final int MAXIMUM_QUANTITY = 99;

    private UUID id;
    private UUID productId;
    private UUID variantId;
    private String productName;
    private String stockKeepingUnit;
    private Money unitPrice;
    private VatRate vatRate;
    private int quantity;

    @Builder.Default
    private List<CartItemAddOn> addOns = new ArrayList<>();

    private Money lineTotal;

    @Builder.Default
    private boolean available = true;

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
                .vatRate(variant.getVatRate())
                .quantity(quantity)
                .addOns(addOns)
                .available(true)
                .build();
        item.recomputeLineTotal();
        return item;
    }

    public static void requireValidQuantity(int quantity) {
        if (quantity < 1 || quantity > MAXIMUM_QUANTITY) {
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
        requireValidQuantity(quantity + other.quantity);
        for (CartItemAddOn otherAddOn : other.addOns) {
            findAddOn(otherAddOn.getProductId())
                    .ifPresent(addOn -> requireValidQuantity(addOn.getQuantity() + otherAddOn.getQuantity()));
        }
        this.quantity += other.quantity;
        this.unitPrice = other.unitPrice;
        this.vatRate = other.vatRate;
        this.available = other.available;
        for (CartItemAddOn otherAddOn : other.addOns) {
            findAddOn(otherAddOn.getProductId()).ifPresent(addOn -> {
                addOn.setQuantity(addOn.getQuantity() + otherAddOn.getQuantity());
                addOn.updatePrice(otherAddOn.getPrice(), otherAddOn.getVatRate());
            });
        }
        recomputeLineTotal();
    }

    public boolean refreshFrom(ProductDetails product) {
        Optional<VariantDetails> variant = product.findVariant(variantId);
        boolean changed = variant
                .map(current -> updatePrice(current.getPrice(), current.getVatRate()))
                .orElse(false);
        boolean everythingAvailable = variant.map(VariantDetails::isAvailable).orElse(false);
        for (CartItemAddOn addOn : addOns) {
            Optional<AddOnDetails> currentAddOn = product.findAddOn(addOn.getProductId());
            changed |= currentAddOn
                    .map(current -> addOn.updatePrice(current.getPrice(), current.getVatRate()))
                    .orElse(false);
            everythingAvailable &= currentAddOn.map(AddOnDetails::isAvailable).orElse(false);
        }
        changed |= changeAvailability(everythingAvailable);
        if (changed) {
            recomputeLineTotal();
        }
        return changed;
    }

    public boolean markUnavailable() {
        return changeAvailability(false);
    }

    private boolean changeAvailability(boolean available) {
        boolean changed = this.available != available;
        this.available = available;
        return changed;
    }

    private boolean updatePrice(Money price, VatRate vatRate) {
        boolean changed = unitPrice.amount().compareTo(price.amount()) != 0 || this.vatRate != vatRate;
        this.unitPrice = price;
        this.vatRate = vatRate;
        return changed;
    }

    private Optional<CartItemAddOn> findAddOn(UUID addOnProductId) {
        return addOns.stream().filter(addOn -> addOn.getProductId().equals(addOnProductId)).findFirst();
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
