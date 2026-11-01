package com.estore.cart;

import lombok.*;

import java.time.Instant;
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
public class Cart {
    private UUID id;
    private CartStatus status;

    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    private Money total;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;

    public static Cart createNew(Instant now, Instant expiresAt) {
        return Cart.builder()
                .id(UUID.randomUUID())
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .total(Money.zero("PLN"))
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(expiresAt)
                .build();
    }

    public void expireIfInactive(Instant now) {
        if (status == CartStatus.ACTIVE && !now.isBefore(expiresAt)) {
            this.status = CartStatus.EXPIRED;
        }
    }

    public void extendExpiration(Instant newExpiresAt) {
        if (status == CartStatus.ACTIVE) {
            this.expiresAt = newExpiresAt;
        }
    }

    public void requireNotExpired() {
        if (status == CartStatus.EXPIRED) {
            throw new CartExpiredException(id);
        }
    }

    public void addItem(CartItem item) {
        requireActive();
        findItemWithSameConfiguration(item).ifPresentOrElse(
                existing -> existing.absorb(item),
                () -> this.items.add(item));
        markChanged();
    }

    public void changeItemQuantity(UUID itemId, int quantity) {
        requireActive();
        findItem(itemId).changeQuantity(quantity);
        markChanged();
    }

    public void removeItem(UUID itemId) {
        requireActive();
        this.items.remove(findItem(itemId));
        markChanged();
    }

    public boolean refresh(CatalogSnapshot catalog) {
        requireActive();
        boolean changed = false;
        for (CartItem item : items) {
            if (catalog.covers(item.getProductId())) {
                changed |= catalog.find(item.getProductId())
                        .map(item::refreshFrom)
                        .orElseGet(item::markUnavailable);
            }
        }
        if (changed) {
            markChanged();
        }
        return changed;
    }

    public Set<UUID> productIds() {
        return items.stream().map(CartItem::getProductId).collect(Collectors.toSet());
    }

    public boolean hasUnavailableItems() {
        return items.stream().anyMatch(item -> !item.isAvailable());
    }

    public void requireVersion(long expectedVersion) {
        if (version != expectedVersion) {
            throw new CartChangedException(id);
        }
    }

    public void requireAllItemsAvailable() {
        List<String> unavailableProducts = items.stream()
                .filter(item -> !item.isAvailable())
                .map(CartItem::getProductName)
                .toList();
        if (!unavailableProducts.isEmpty()) {
            throw new UnavailableItemsException(id, unavailableProducts);
        }
    }

    public boolean isCheckedOut() {
        return status == CartStatus.CHECKED_OUT;
    }

    public void checkout() {
        if (isCheckedOut()) {
            return;
        }
        requireActive();
        if (items.isEmpty()) {
            throw new EmptyCartException(id);
        }
        requireAllItemsAvailable();
        this.status = CartStatus.CHECKED_OUT;
        this.updatedAt = Instant.now();
    }

    private void requireActive() {
        requireNotExpired();
        if (status != CartStatus.ACTIVE) {
            throw new CartNotActiveException(id, status);
        }
    }

    private Optional<CartItem> findItemWithSameConfiguration(CartItem item) {
        return items.stream().filter(existing -> existing.hasSameConfigurationAs(item)).findFirst();
    }

    private CartItem findItem(UUID itemId) {
        return items.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException(itemId));
    }

    private void markChanged() {
        recomputeTotal();
        this.version++;
        this.updatedAt = Instant.now();
    }

    private void recomputeTotal() {
        Money sum = Money.zero("PLN");
        for (CartItem item : items) {
            sum = sum.add(item.getLineTotal());
        }
        this.total = sum;
    }
}
