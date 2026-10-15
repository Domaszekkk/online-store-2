package com.estore.cart;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private Instant createdAt;
    private Instant updatedAt;

    public static Cart createNew() {
        Instant now = Instant.now();
        return Cart.builder()
                .id(UUID.randomUUID())
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .total(Money.zero("PLN"))
                .createdAt(now)
                .updatedAt(now)
                .build();
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

    private void requireActive() {
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
