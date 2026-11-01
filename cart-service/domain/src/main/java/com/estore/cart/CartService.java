package com.estore.cart;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class CartService {
    private final CartRepositoryPort cartRepositoryPort;
    private final ProductCatalogPort productCatalogPort;
    private final TransactionPort transactionPort;
    private final Clock clock;
    private final Duration inactivityTimeout;
    private final Duration expiredCartRetention;

    public Cart createCart() {
        Instant now = clock.instant();
        return cartRepositoryPort.save(Cart.createNew(now, now.plus(inactivityTimeout)));
    }

    public Cart getCartById(UUID id) {
        Cart cart = cartRepositoryPort
                .findById(id)
                .orElseThrow(() -> new CartNotFoundException(id));
        cart.expireIfInactive(clock.instant());
        return cart;
    }

    public int expireInactiveCarts() {
        return cartRepositoryPort.markExpiredCarts(clock.instant());
    }

    public long deleteExpiredCarts() {
        return cartRepositoryPort.deleteExpiredCartsBefore(clock.instant().minus(expiredCartRetention));
    }

    public Cart addItem(UUID cartId, AddItemCommand command) {
        CartItem.requireValidQuantity(command.getQuantity());
        getCartById(cartId).requireNotExpired();

        ProductDetails product = productCatalogPort.getProduct(command.getProductId());
        VariantDetails variant = product.findVariant(command.getVariantId())
                .orElseThrow(() -> new VariantNotFoundException(command.getVariantId()));
        if (!variant.isAvailable()) {
            throw new VariantNotAvailableException(command.getVariantId());
        }
        List<CartItemAddOn> addOns = resolveAddOns(product, command.getAddOns());

        return modifyCart(cartId, cart -> cart.addItem(
                CartItem.create(product.getId(), product.getName(), variant, command.getQuantity(), addOns)));
    }

    public Cart changeItemQuantity(UUID cartId, UUID itemId, ChangeItemQuantityCommand command) {
        return modifyCart(cartId, cart -> cart.changeItemQuantity(itemId, command.getQuantity()));
    }

    public Cart removeItem(UUID cartId, UUID itemId) {
        return modifyCart(cartId, cart -> cart.removeItem(itemId));
    }

    public Cart refresh(UUID cartId) {
        Cart current = getCartById(cartId);
        if (current.isCheckedOut()) {
            return current;
        }
        current.requireNotExpired();
        CatalogSnapshot catalog = catalogFor(current);
        return transactionPort.inTransaction(() -> {
            Cart cart = getCartForUpdate(cartId);
            if (cart.isCheckedOut()) {
                return cart;
            }
            cart.refresh(catalog);
            cart.extendExpiration(clock.instant().plus(inactivityTimeout));
            return cartRepositoryPort.save(cart);
        });
    }

    public Cart checkout(UUID cartId, long expectedVersion) {
        Cart current = getCartById(cartId);
        if (current.isCheckedOut()) {
            current.requireVersion(expectedVersion);
            return current;
        }
        current.requireNotExpired();
        CatalogSnapshot catalog = catalogFor(current);
        Cart cart = transactionPort.inTransaction(() -> {
            Cart lockedCart = getCartForUpdate(cartId);
            if (lockedCart.isCheckedOut()) {
                return lockedCart;
            }
            lockedCart.requireVersion(expectedVersion);
            boolean changed = lockedCart.refresh(catalog);
            if (!changed && !lockedCart.hasUnavailableItems()) {
                lockedCart.checkout();
            }
            return changed || lockedCart.isCheckedOut() ? cartRepositoryPort.save(lockedCart) : lockedCart;
        });
        cart.requireVersion(expectedVersion);
        cart.requireAllItemsAvailable();
        return cart;
    }

    private CatalogSnapshot catalogFor(Cart cart) {
        Set<UUID> productIds = cart.productIds();
        Map<UUID, ProductDetails> products = new HashMap<>();
        for (UUID productId : productIds) {
            productCatalogPort.findProduct(productId).ifPresent(product -> products.put(productId, product));
        }
        return new CatalogSnapshot(productIds, products);
    }

    private Cart modifyCart(UUID cartId, Consumer<Cart> modification) {
        return transactionPort.inTransaction(() -> {
            Cart cart = getCartForUpdate(cartId);
            modification.accept(cart);
            cart.extendExpiration(clock.instant().plus(inactivityTimeout));
            return cartRepositoryPort.save(cart);
        });
    }

    private Cart getCartForUpdate(UUID cartId) {
        Cart cart = cartRepositoryPort
                .findByIdForUpdate(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));
        cart.expireIfInactive(clock.instant());
        return cart;
    }

    private List<CartItemAddOn> resolveAddOns(ProductDetails product, List<AddOnSelection> selections) {
        if (selections == null || selections.isEmpty()) {
            return new ArrayList<>();
        }
        Map<UUID, Integer> quantityPerAddOn = new LinkedHashMap<>();
        for (AddOnSelection selection : selections) {
            CartItem.requireValidQuantity(selection.getQuantity());
            quantityPerAddOn.merge(selection.getProductId(), selection.getQuantity(), Integer::sum);
        }
        List<CartItemAddOn> addOns = new ArrayList<>();
        quantityPerAddOn.forEach((addOnProductId, quantity) -> {
            CartItem.requireValidQuantity(quantity);
            AddOnDetails addOn = product.findAddOn(addOnProductId)
                    .orElseThrow(() -> new ProductNotFoundException(addOnProductId));
            if (!addOn.isAvailable()) {
                throw new AddOnNotAvailableException(addOnProductId);
            }
            addOns.add(CartItemAddOn.builder()
                    .productId(addOn.getProductId())
                    .name(addOn.getName())
                    .price(addOn.getPrice())
                    .vatRate(addOn.getVatRate())
                    .quantity(quantity)
                    .build());
        });
        return addOns;
    }
}
