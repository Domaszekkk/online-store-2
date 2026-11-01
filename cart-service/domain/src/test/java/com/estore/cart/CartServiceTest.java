package com.estore.cart;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");
    private static final Duration INACTIVITY_TIMEOUT = Duration.ofMinutes(30);

    private CartRepositoryPort cartRepositoryPort;
    private ProductCatalogPort productCatalogPort;
    private CartService cartService;
    private Instant currentTime;

    @BeforeEach
    void setup() {
        this.cartRepositoryPort = Mockito.mock(CartRepositoryPort.class);
        this.productCatalogPort = Mockito.mock(ProductCatalogPort.class);
        this.currentTime = NOW;
        TransactionPort transactionPort = Mockito.mock(TransactionPort.class);
        when(transactionPort.inTransaction(any())).thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
        when(cartRepositoryPort.findByIdForUpdate(any()))
                .thenAnswer(invocation -> cartRepositoryPort.findById(invocation.getArgument(0)));
        Clock clock = Mockito.mock(Clock.class);
        when(clock.instant()).thenAnswer(invocation -> currentTime);
        this.cartService = new CartService(cartRepositoryPort, productCatalogPort, transactionPort,
                clock, INACTIVITY_TIMEOUT, Duration.ofDays(7));
    }

    private Cart emptyCart(UUID id) {
        Cart cart = Cart.createNew(NOW, NOW.plus(INACTIVITY_TIMEOUT));
        cart.setId(id);
        return cart;
    }

    private ProductDetails product(UUID productId, UUID variantId, String availability) {
        return ProductDetails.builder()
                .id(productId).name("ProBook X15")
                .variants(List.of(VariantDetails.builder()
                        .id(variantId).stockKeepingUnit("PBX15-I7-16")
                        .price(Money.of(new BigDecimal("4599.00"), "PLN"))
                        .availability(availability).vatRate(VatRate.VAT_23).build()))
                .build();
    }

    @Test
    void createCart_ReturnsActiveCart() {
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart result = cartService.createCart();

        assertAll(
                () -> assertNotNull(result.getId()),
                () -> assertEquals(CartStatus.ACTIVE, result.getStatus()),
                () -> assertEquals("PLN", result.getTotal().currency()),
                () -> assertEquals(NOW.plus(INACTIVITY_TIMEOUT), result.getExpiresAt())
        );
    }

    @Test
    void getCartById_CartNotFound_ThrowsCartNotFoundException() {
        UUID id = UUID.randomUUID();
        when(cartRepositoryPort.findById(id)).thenReturn(Optional.empty());

        CartNotFoundException exception = Assertions.assertThrows(
                CartNotFoundException.class, () -> cartService.getCartById(id));

        assertEquals(404, exception.getResponseCode());
    }

    @Test
    void addItem_DataCorrect_AddsItemAndUpdatesTotal() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        when(productCatalogPort.getProduct(productId)).thenReturn(product(productId, variantId, "AVAILABLE"));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AddItemCommand command = AddItemCommand.builder()
                .productId(productId).variantId(variantId).quantity(2).build();

        Cart result = cartService.addItem(cartId, command);

        assertAll(
                () -> assertEquals(1, result.getItems().size()),
                () -> assertEquals(2, result.getItems().get(0).getQuantity()),
                () -> assertEquals("PBX15-I7-16", result.getItems().get(0).getStockKeepingUnit()),
                () -> assertEquals(new BigDecimal("9198.00"), result.getItems().get(0).getLineTotal().amount()),
                () -> assertEquals(new BigDecimal("9198.00"), result.getTotal().amount())
        );
    }

    @Test
    void addItem_VariantNotAvailable_ThrowsVariantNotAvailableException() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        when(productCatalogPort.getProduct(productId)).thenReturn(product(productId, variantId, "COMING_SOON"));
        AddItemCommand command = AddItemCommand.builder()
                .productId(productId).variantId(variantId).quantity(1).build();

        VariantNotAvailableException exception = Assertions.assertThrows(
                VariantNotAvailableException.class, () -> cartService.addItem(cartId, command));

        assertEquals(409, exception.getResponseCode());
    }

    @Test
    void addItem_VariantNotFound_ThrowsVariantNotFoundException() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        when(productCatalogPort.getProduct(productId)).thenReturn(product(productId, UUID.randomUUID(), "AVAILABLE"));
        AddItemCommand command = AddItemCommand.builder()
                .productId(productId).variantId(variantId).quantity(1).build();

        VariantNotFoundException exception = Assertions.assertThrows(
                VariantNotFoundException.class, () -> cartService.addItem(cartId, command));

        assertEquals(404, exception.getResponseCode());
    }

    private ProductDetails productWithAddOn(UUID productId, UUID variantId, UUID addOnProductId) {
        ProductDetails product = product(productId, variantId, "AVAILABLE");
        product.setAddOns(List.of(AddOnDetails.builder()
                .productId(addOnProductId).name("Laptop bag")
                .price(Money.of(new BigDecimal("199.00"), "PLN")).vatRate(VatRate.VAT_8).availability("AVAILABLE").build()));
        return product;
    }

    private Cart cartWithProduct(UUID cartId, UUID productId, UUID variantId) {
        Cart cart = emptyCart(cartId);
        cart.addItem(CartItem.create(productId, "ProBook X15",
                product(productId, variantId, "AVAILABLE").getVariants().get(0), 1, new ArrayList<>()));
        return cart;
    }

    private Cart cartWithItem(UUID cartId, UUID itemId, int quantity) {
        Cart cart = emptyCart(cartId);
        CartItem item = CartItem.create(UUID.randomUUID(), "ProBook X15",
                VariantDetails.builder().id(UUID.randomUUID()).stockKeepingUnit("PBX15-I7-16")
                        .price(Money.of(new BigDecimal("4599.00"), "PLN")).availability("AVAILABLE").vatRate(VatRate.VAT_23).build(),
                quantity, List.of());
        item.setId(itemId);
        cart.addItem(item);
        return cart;
    }

    private static void assertAmount(String expected, Money actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.amount()),
                "expected " + expected + " but was " + actual.amount());
    }

    @Test
    void addItem_SameConfigurationTwice_IncreasesQuantityOfExistingItem() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        when(productCatalogPort.getProduct(productId)).thenReturn(product(productId, variantId, "AVAILABLE"));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AddItemCommand command = AddItemCommand.builder()
                .productId(productId).variantId(variantId).quantity(1).build();

        cartService.addItem(cartId, command);
        Cart result = cartService.addItem(cartId, command);

        assertAll(
                () -> assertEquals(1, result.getItems().size()),
                () -> assertEquals(2, result.getItems().get(0).getQuantity()),
                () -> assertAmount("9198.00", result.getTotal())
        );
    }

    @Test
    void addItem_QuantityAboveLimit_ThrowsInvalidQuantityException() {
        AddItemCommand command = AddItemCommand.builder()
                .productId(UUID.randomUUID()).variantId(UUID.randomUUID()).quantity(100).build();

        InvalidQuantityException exception = Assertions.assertThrows(
                InvalidQuantityException.class, () -> cartService.addItem(UUID.randomUUID(), command));

        assertAll(
                () -> assertEquals(400, exception.getResponseCode()),
                () -> assertEquals("Quantity must be between 1 and 99, but was 100", exception.getMessage())
        );
        verify(cartRepositoryPort, never()).save(any());
    }

    @Test
    void addItem_SameConfigurationAboveLimitInTotal_ThrowsInvalidQuantityException() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        when(productCatalogPort.getProduct(productId)).thenReturn(product(productId, variantId, "AVAILABLE"));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AddItemCommand command = AddItemCommand.builder()
                .productId(productId).variantId(variantId).quantity(60).build();
        Cart cart = cartService.addItem(cartId, command);
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(cart));

        InvalidQuantityException exception = Assertions.assertThrows(
                InvalidQuantityException.class, () -> cartService.addItem(cartId, command));

        assertAll(
                () -> assertEquals("Quantity must be between 1 and 99, but was 120", exception.getMessage()),
                () -> assertEquals(60, cart.getItems().get(0).getQuantity())
        );
    }

    @Test
    void addItem_SameVariantWithDifferentAddOns_CreatesSeparateItem() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID addOnProductId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        when(productCatalogPort.getProduct(productId)).thenReturn(productWithAddOn(productId, variantId, addOnProductId));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cartService.addItem(cartId, AddItemCommand.builder()
                .productId(productId).variantId(variantId).quantity(1).build());
        Cart result = cartService.addItem(cartId, AddItemCommand.builder()
                .productId(productId).variantId(variantId).quantity(1)
                .addOns(List.of(new AddOnSelection(addOnProductId, 1))).build());

        assertEquals(2, result.getItems().size());
    }

    @Test
    void addItem_TwoLaptopsOneCable_ChargesAddOnByItsOwnQuantity() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID addOnProductId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        when(productCatalogPort.getProduct(productId)).thenReturn(productWithAddOn(productId, variantId, addOnProductId));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AddItemCommand command = AddItemCommand.builder()
                .productId(productId).variantId(variantId).quantity(2)
                .addOns(List.of(new AddOnSelection(addOnProductId, 1))).build();

        Cart result = cartService.addItem(cartId, command);

        CartItem item = result.getItems().get(0);
        assertAll(
                () -> assertEquals(2, item.getQuantity()),
                () -> assertEquals(1, item.getAddOns().get(0).getQuantity()),
                () -> assertAmount("9397.00", item.getLineTotal()),
                () -> assertAmount("9397.00", result.getTotal()),
                () -> assertEquals(VatRate.VAT_23, item.getVatRate()),
                () -> assertEquals(VatRate.VAT_8, item.getAddOns().get(0).getVatRate())
        );
    }

    @Test
    void addItem_SameAddOnListedTwice_SumsAddOnQuantity() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID addOnProductId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        when(productCatalogPort.getProduct(productId)).thenReturn(productWithAddOn(productId, variantId, addOnProductId));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AddItemCommand command = AddItemCommand.builder()
                .productId(productId).variantId(variantId).quantity(1)
                .addOns(List.of(new AddOnSelection(addOnProductId, 1), new AddOnSelection(addOnProductId, 2))).build();

        Cart result = cartService.addItem(cartId, command);

        assertAll(
                () -> assertEquals(1, result.getItems().get(0).getAddOns().size()),
                () -> assertEquals(3, result.getItems().get(0).getAddOns().get(0).getQuantity())
        );
    }

    @Test
    void addItem_SameConfigurationTwice_SumsItemAndAddOnQuantities() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID addOnProductId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        when(productCatalogPort.getProduct(productId)).thenReturn(productWithAddOn(productId, variantId, addOnProductId));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AddItemCommand command = AddItemCommand.builder()
                .productId(productId).variantId(variantId).quantity(2)
                .addOns(List.of(new AddOnSelection(addOnProductId, 1))).build();

        cartService.addItem(cartId, command);
        Cart result = cartService.addItem(cartId, command);

        CartItem item = result.getItems().get(0);
        assertAll(
                () -> assertEquals(1, result.getItems().size()),
                () -> assertEquals(4, item.getQuantity()),
                () -> assertEquals(2, item.getAddOns().get(0).getQuantity()),
                () -> assertAmount("18794.00", result.getTotal())
        );
    }

    @Test
    void addItem_AddOnQuantityZero_ThrowsInvalidQuantityException() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID addOnProductId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        when(productCatalogPort.getProduct(productId)).thenReturn(productWithAddOn(productId, variantId, addOnProductId));
        AddItemCommand command = AddItemCommand.builder()
                .productId(productId).variantId(variantId).quantity(1)
                .addOns(List.of(new AddOnSelection(addOnProductId, 0))).build();

        Assertions.assertThrows(InvalidQuantityException.class, () -> cartService.addItem(cartId, command));
    }

    @Test
    void changeItemQuantity_ItemWithAddOn_KeepsAddOnQuantity() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID addOnProductId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        when(productCatalogPort.getProduct(productId)).thenReturn(productWithAddOn(productId, variantId, addOnProductId));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Cart cart = cartService.addItem(cartId, AddItemCommand.builder()
                .productId(productId).variantId(variantId).quantity(1)
                .addOns(List.of(new AddOnSelection(addOnProductId, 1))).build());
        UUID itemId = cart.getItems().get(0).getId();

        Cart result = cartService.changeItemQuantity(cartId, itemId, new ChangeItemQuantityCommand(3));

        assertAll(
                () -> assertEquals(1, result.getItems().get(0).getAddOns().get(0).getQuantity()),
                () -> assertAmount("13996.00", result.getTotal())
        );
    }

    @Test
    void addItem_CartCheckedOut_ThrowsCartNotActiveException() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Cart checkedOutCart = emptyCart(cartId);
        checkedOutCart.setStatus(CartStatus.CHECKED_OUT);
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(checkedOutCart));
        when(productCatalogPort.getProduct(productId)).thenReturn(product(productId, variantId, "AVAILABLE"));
        AddItemCommand command = AddItemCommand.builder()
                .productId(productId).variantId(variantId).quantity(1).build();

        CartNotActiveException exception = Assertions.assertThrows(
                CartNotActiveException.class, () -> cartService.addItem(cartId, command));

        assertEquals(409, exception.getResponseCode());
    }

    @Test
    void addItem_QuantityZero_ThrowsInvalidQuantityException() {
        AddItemCommand command = AddItemCommand.builder()
                .productId(UUID.randomUUID()).variantId(UUID.randomUUID()).quantity(0).build();

        InvalidQuantityException exception = Assertions.assertThrows(
                InvalidQuantityException.class, () -> cartService.addItem(UUID.randomUUID(), command));

        assertEquals(400, exception.getResponseCode());
    }

    @Test
    void changeItemQuantity_DataCorrect_RecomputesLineTotalAndTotal() {
        UUID cartId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(cartWithItem(cartId, itemId, 1)));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart result = cartService.changeItemQuantity(cartId, itemId, new ChangeItemQuantityCommand(3));

        assertAll(
                () -> assertEquals(3, result.getItems().get(0).getQuantity()),
                () -> assertAmount("13797.00", result.getItems().get(0).getLineTotal()),
                () -> assertAmount("13797.00", result.getTotal())
        );
    }

    @Test
    void changeItemQuantity_ItemNotFound_ThrowsCartItemNotFoundException() {
        UUID cartId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(cartWithItem(cartId, UUID.randomUUID(), 1)));

        CartItemNotFoundException exception = Assertions.assertThrows(CartItemNotFoundException.class,
                () -> cartService.changeItemQuantity(cartId, UUID.randomUUID(), new ChangeItemQuantityCommand(2)));

        assertEquals(404, exception.getResponseCode());
    }

    @Test
    void changeItemQuantity_QuantityZero_ThrowsInvalidQuantityException() {
        UUID cartId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(cartWithItem(cartId, itemId, 1)));

        Assertions.assertThrows(InvalidQuantityException.class,
                () -> cartService.changeItemQuantity(cartId, itemId, new ChangeItemQuantityCommand(0)));
    }

    @Test
    void removeItem_DataCorrect_RemovesItemAndResetsTotal() {
        UUID cartId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(cartWithItem(cartId, itemId, 2)));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart result = cartService.removeItem(cartId, itemId);

        assertAll(
                () -> assertTrue(result.getItems().isEmpty()),
                () -> assertAmount("0", result.getTotal())
        );
    }

    @Test
    void removeItem_CartCheckedOut_ThrowsCartNotActiveException() {
        UUID cartId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Cart cart = cartWithItem(cartId, itemId, 1);
        cart.setStatus(CartStatus.CHECKED_OUT);
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(cart));

        Assertions.assertThrows(CartNotActiveException.class, () -> cartService.removeItem(cartId, itemId));
    }

    @Test
    void addItem_AddOnSoldOut_ThrowsAddOnNotAvailableException() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID addOnProductId = UUID.randomUUID();
        ProductDetails product = productWithAddOn(productId, variantId, addOnProductId);
        product.getAddOns().get(0).setAvailability("SOLD_OUT");
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        when(productCatalogPort.getProduct(productId)).thenReturn(product);
        AddItemCommand command = AddItemCommand.builder()
                .productId(productId).variantId(variantId).quantity(1)
                .addOns(List.of(AddOnSelection.builder().productId(addOnProductId).quantity(1).build()))
                .build();

        AddOnNotAvailableException exception = Assertions.assertThrows(
                AddOnNotAvailableException.class, () -> cartService.addItem(cartId, command));

        assertEquals(409, exception.getResponseCode());
        verify(cartRepositoryPort, never()).save(any());
    }

    @Test
    void addItem_EveryChange_IncreasesVersion() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        when(productCatalogPort.getProduct(productId)).thenReturn(product(productId, variantId, "AVAILABLE"));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AddItemCommand command = AddItemCommand.builder()
                .productId(productId).variantId(variantId).quantity(1).build();

        cartService.addItem(cartId, command);
        Cart result = cartService.addItem(cartId, command);

        assertEquals(2, result.getVersion());
    }

    @Test
    void checkout_CartWithItems_ChangesStatusToCheckedOut() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Cart cart = cartWithProduct(cartId, productId, variantId);
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(cart));
        when(productCatalogPort.findProduct(productId)).thenReturn(Optional.of(product(productId, variantId, "AVAILABLE")));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart result = cartService.checkout(cartId, 1);

        assertAll(
                () -> assertEquals(CartStatus.CHECKED_OUT, result.getStatus()),
                () -> assertEquals(1, result.getItems().size()),
                () -> assertEquals(1, result.getVersion())
        );
    }

    @Test
    void checkout_CartChangedAfterSummary_ThrowsCartChangedException() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(cartWithProduct(cartId, productId, variantId)));
        when(productCatalogPort.findProduct(productId)).thenReturn(Optional.of(product(productId, variantId, "AVAILABLE")));

        CartChangedException exception = Assertions.assertThrows(
                CartChangedException.class, () -> cartService.checkout(cartId, 0));

        assertEquals(409, exception.getResponseCode());
        verify(cartRepositoryPort, never()).save(any());
    }

    @Test
    void checkout_ProductWithdrawnFromSale_MarksItemUnavailableAndThrowsCartChangedException() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Cart cart = cartWithProduct(cartId, productId, UUID.randomUUID());
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(cart));
        when(productCatalogPort.findProduct(productId)).thenReturn(Optional.empty());
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Assertions.assertThrows(CartChangedException.class, () -> cartService.checkout(cartId, 1));

        assertAll(
                () -> assertEquals(CartStatus.ACTIVE, cart.getStatus()),
                () -> assertEquals(false, cart.getItems().get(0).isAvailable()),
                () -> assertEquals(2, cart.getVersion())
        );
        verify(cartRepositoryPort).save(cart);
    }

    @Test
    void checkout_PriceChanged_UpdatesPriceAndThrowsCartChangedException() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Cart cart = cartWithProduct(cartId, productId, variantId);
        ProductDetails product = product(productId, variantId, "AVAILABLE");
        product.getVariants().get(0).setPrice(Money.of(new BigDecimal("4999.00"), "PLN"));
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(cart));
        when(productCatalogPort.findProduct(productId)).thenReturn(Optional.of(product));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Assertions.assertThrows(CartChangedException.class, () -> cartService.checkout(cartId, 1));

        assertAll(
                () -> assertAmount("4999.00", cart.getItems().get(0).getUnitPrice()),
                () -> assertAmount("4999.00", cart.getTotal()),
                () -> assertEquals(CartStatus.ACTIVE, cart.getStatus())
        );
    }

    @Test
    void checkout_ItemStillUnavailable_ThrowsUnavailableItemsException() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Cart cart = cartWithProduct(cartId, productId, variantId);
        cart.getItems().get(0).setAvailable(false);
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(cart));
        when(productCatalogPort.findProduct(productId)).thenReturn(Optional.of(product(productId, variantId, "SOLD_OUT")));

        UnavailableItemsException exception = Assertions.assertThrows(
                UnavailableItemsException.class, () -> cartService.checkout(cartId, 1));

        assertAll(
                () -> assertEquals(409, exception.getResponseCode()),
                () -> assertTrue(exception.getMessage().contains("ProBook X15")),
                () -> assertEquals(CartStatus.ACTIVE, cart.getStatus())
        );
    }

    @Test
    void refresh_AddOnSoldOut_MarksItemUnavailable() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID addOnProductId = UUID.randomUUID();
        ProductDetails product = productWithAddOn(productId, variantId, addOnProductId);
        Cart cart = emptyCart(cartId);
        cart.addItem(CartItem.create(productId, "ProBook X15", product.getVariants().get(0), 1,
                new ArrayList<>(List.of(CartItemAddOn.builder().productId(addOnProductId).name("Laptop bag")
                        .price(Money.of(new BigDecimal("199.00"), "PLN")).vatRate(VatRate.VAT_8).quantity(1).build()))));
        product.getAddOns().get(0).setAvailability("SOLD_OUT");
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(cart));
        when(productCatalogPort.findProduct(productId)).thenReturn(Optional.of(product));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart result = cartService.refresh(cartId);

        assertAll(
                () -> assertEquals(false, result.getItems().get(0).isAvailable()),
                () -> assertEquals(2, result.getVersion())
        );
    }

    @Test
    void refresh_NothingChanged_KeepsVersionAndExtendsExpiration() {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(cartWithProduct(cartId, productId, variantId)));
        when(productCatalogPort.findProduct(productId)).thenReturn(Optional.of(product(productId, variantId, "AVAILABLE")));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        currentTime = NOW.plus(Duration.ofMinutes(20));

        Cart result = cartService.refresh(cartId);

        assertAll(
                () -> assertEquals(1, result.getVersion()),
                () -> assertEquals(NOW.plus(Duration.ofMinutes(50)), result.getExpiresAt())
        );
    }

    @Test
    void getCartById_InactiveFor30Minutes_ReturnsExpiredCart() {
        UUID cartId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        currentTime = NOW.plus(INACTIVITY_TIMEOUT);

        Cart result = cartService.getCartById(cartId);

        assertEquals(CartStatus.EXPIRED, result.getStatus());
    }

    @Test
    void addItem_CartInactiveFor30Minutes_ThrowsCartExpiredExceptionWithoutAskingCatalog() {
        UUID cartId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));
        currentTime = NOW.plus(Duration.ofMinutes(31));
        AddItemCommand command = AddItemCommand.builder()
                .productId(UUID.randomUUID()).variantId(UUID.randomUUID()).quantity(1).build();

        CartExpiredException exception = Assertions.assertThrows(
                CartExpiredException.class, () -> cartService.addItem(cartId, command));

        assertEquals(409, exception.getResponseCode());
        verify(productCatalogPort, never()).getProduct(any());
        verify(cartRepositoryPort, never()).save(any());
    }

    @Test
    void changeItemQuantity_ActiveCart_ExtendsExpirationFromNow() {
        UUID cartId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(cartWithItem(cartId, itemId, 1)));
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        currentTime = NOW.plus(Duration.ofMinutes(29));

        Cart result = cartService.changeItemQuantity(cartId, itemId, new ChangeItemQuantityCommand(2));

        assertEquals(NOW.plus(Duration.ofMinutes(59)), result.getExpiresAt());
    }

    @Test
    void checkout_CartExpired_ThrowsCartExpiredException() {
        UUID cartId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId))
                .thenReturn(Optional.of(cartWithProduct(cartId, UUID.randomUUID(), UUID.randomUUID())));
        currentTime = NOW.plus(Duration.ofHours(1));

        Assertions.assertThrows(CartExpiredException.class, () -> cartService.checkout(cartId, 1));

        verify(productCatalogPort, never()).findProduct(any());
    }

    @Test
    void checkout_CheckedOutCartLongAgo_StillReturnsItsContent() {
        UUID cartId = UUID.randomUUID();
        Cart checkedOutCart = cartWithItem(cartId, UUID.randomUUID(), 1);
        checkedOutCart.checkout();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(checkedOutCart));
        currentTime = NOW.plus(Duration.ofDays(1));

        Cart result = cartService.checkout(cartId, 1);

        assertEquals(CartStatus.CHECKED_OUT, result.getStatus());
    }

    @Test
    void expireInactiveCartsAndDeleteExpiredCarts_UseCurrentTimeAndRetention() {
        when(cartRepositoryPort.markExpiredCarts(NOW)).thenReturn(3);
        when(cartRepositoryPort.deleteExpiredCartsBefore(NOW.minus(Duration.ofDays(7)))).thenReturn(2L);

        assertAll(
                () -> assertEquals(3, cartService.expireInactiveCarts()),
                () -> assertEquals(2L, cartService.deleteExpiredCarts())
        );
    }

    @Test
    void checkout_EmptyCart_ThrowsEmptyCartException() {
        UUID cartId = UUID.randomUUID();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(emptyCart(cartId)));

        EmptyCartException exception = Assertions.assertThrows(
                EmptyCartException.class, () -> cartService.checkout(cartId, 0));

        assertEquals(409, exception.getResponseCode());
    }

    @Test
    void checkout_CartAlreadyCheckedOut_ReturnsSameContentWithoutSaving() {
        UUID cartId = UUID.randomUUID();
        Cart checkedOutCart = cartWithItem(cartId, UUID.randomUUID(), 2);
        checkedOutCart.checkout();
        Instant checkedOutAt = checkedOutCart.getUpdatedAt();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(checkedOutCart));

        Cart result = cartService.checkout(cartId, 1);

        assertAll(
                () -> assertEquals(CartStatus.CHECKED_OUT, result.getStatus()),
                () -> assertEquals(1, result.getItems().size()),
                () -> assertEquals(2, result.getItems().get(0).getQuantity()),
                () -> assertEquals(checkedOutAt, result.getUpdatedAt())
        );
        verify(cartRepositoryPort, never()).save(any());
    }

    @Test
    void changeItemQuantity_CartAlreadyCheckedOut_ThrowsCartNotActiveException() {
        UUID cartId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Cart checkedOutCart = cartWithItem(cartId, itemId, 1);
        checkedOutCart.checkout();
        when(cartRepositoryPort.findById(cartId)).thenReturn(Optional.of(checkedOutCart));

        CartNotActiveException exception = Assertions.assertThrows(CartNotActiveException.class,
                () -> cartService.changeItemQuantity(cartId, itemId, new ChangeItemQuantityCommand(3)));

        assertEquals(409, exception.getResponseCode());
    }
}
