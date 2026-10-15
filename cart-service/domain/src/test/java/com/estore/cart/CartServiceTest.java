package com.estore.cart;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CartServiceTest {

    private CartRepositoryPort cartRepositoryPort;
    private ProductCatalogPort productCatalogPort;
    private CartService cartService;

    @BeforeEach
    void setup() {
        this.cartRepositoryPort = Mockito.mock(CartRepositoryPort.class);
        this.productCatalogPort = Mockito.mock(ProductCatalogPort.class);
        this.cartService = new CartService(cartRepositoryPort, productCatalogPort);
    }

    private Cart emptyCart(UUID id) {
        Instant now = Instant.now();
        return Cart.builder()
                .id(id).status(CartStatus.ACTIVE).items(new ArrayList<>())
                .total(Money.zero("PLN")).createdAt(now).updatedAt(now).build();
    }

    private ProductDetails product(UUID productId, UUID variantId, String availability) {
        return ProductDetails.builder()
                .id(productId).name("ProBook X15")
                .variants(List.of(VariantDetails.builder()
                        .id(variantId).stockKeepingUnit("PBX15-I7-16")
                        .price(Money.of(new BigDecimal("4599.00"), "PLN"))
                        .availability(availability).build()))
                .build();
    }

    @Test
    void createCart_ReturnsActiveCart() {
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart result = cartService.createCart();

        assertAll(
                () -> assertNotNull(result.getId()),
                () -> assertEquals(CartStatus.ACTIVE, result.getStatus()),
                () -> assertEquals("PLN", result.getTotal().currency())
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
                .price(Money.of(new BigDecimal("199.00"), "PLN")).build()));
        return product;
    }

    private Cart cartWithItem(UUID cartId, UUID itemId, int quantity) {
        Cart cart = emptyCart(cartId);
        CartItem item = CartItem.create(UUID.randomUUID(), "ProBook X15",
                VariantDetails.builder().id(UUID.randomUUID()).stockKeepingUnit("PBX15-I7-16")
                        .price(Money.of(new BigDecimal("4599.00"), "PLN")).availability("AVAILABLE").build(),
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
                // 2 x 4599.00 + 1 x 199.00
                () -> assertAmount("9397.00", item.getLineTotal()),
                () -> assertAmount("9397.00", result.getTotal())
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
                // 4 x 4599.00 + 2 x 199.00
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
                // 3 x 4599.00 + 1 x 199.00
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
}
