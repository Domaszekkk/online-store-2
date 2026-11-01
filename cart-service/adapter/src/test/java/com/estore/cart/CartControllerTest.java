package com.estore.cart;

import com.estore.cart.cart.CartController;
import com.estore.cart.cart.CartMapperImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import(CartMapperImpl.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CartService cartService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Cart emptyCart(UUID id) {
        Instant now = Instant.now();
        return Cart.builder()
                .id(id).status(CartStatus.ACTIVE).items(new ArrayList<>())
                .total(Money.zero("PLN")).createdAt(now).updatedAt(now).build();
    }

    private Cart cartWithItem(UUID id) {
        CartItem item = CartItem.builder()
                .id(UUID.randomUUID()).productId(UUID.randomUUID()).variantId(UUID.randomUUID())
                .productName("ProBook X15").stockKeepingUnit("PBX15-I7-16")
                .unitPrice(Money.of(new BigDecimal("4599.00"), "PLN")).quantity(2)
                .addOns(new ArrayList<>()).lineTotal(Money.of(new BigDecimal("9198.00"), "PLN"))
                .build();
        Instant now = Instant.now();
        return Cart.builder()
                .id(id).status(CartStatus.ACTIVE).items(List.of(item))
                .total(Money.of(new BigDecimal("9198.00"), "PLN")).createdAt(now).updatedAt(now).build();
    }

    @Test
    void createCart_Returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(cartService.createCart()).thenReturn(emptyCart(id));

        mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.total.currency").value("PLN"));
    }

    @Test
    void getCartById_DataCorrect_Returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(cartService.getCartById(id)).thenReturn(emptyCart(id));

        mockMvc.perform(get("/carts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getCartById_CartNotFound_Returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(cartService.getCartById(id)).thenThrow(new CartNotFoundException(id));

        mockMvc.perform(get("/carts/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cart with id " + id + " not found"));
    }

    @Test
    void addItem_DataCorrect_Returns200() throws Exception {
        UUID cartId = UUID.randomUUID();
        when(cartService.addItem(eq(cartId), any(AddItemCommand.class))).thenReturn(cartWithItem(cartId));
        AddItemRequest request = AddItemRequest.builder()
                .productId(UUID.randomUUID()).variantId(UUID.randomUUID()).quantity(2).build();

        mockMvc.perform(post("/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].stockKeepingUnit").value("PBX15-I7-16"))
                .andExpect(jsonPath("$.total.currency").value("PLN"));
    }

    @Test
    void addItem_ProductServiceUnavailable_Returns503() throws Exception {
        UUID cartId = UUID.randomUUID();
        when(cartService.addItem(eq(cartId), any(AddItemCommand.class)))
                .thenThrow(new ProductCatalogUnavailableException());
        AddItemRequest request = AddItemRequest.builder()
                .productId(UUID.randomUUID()).variantId(UUID.randomUUID()).quantity(1).build();

        mockMvc.perform(post("/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void changeItemQuantity_DataCorrect_Returns200() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(cartService.changeItemQuantity(eq(cartId), eq(itemId), any(ChangeItemQuantityCommand.class)))
                .thenReturn(cartWithItem(cartId));

        mockMvc.perform(patch("/carts/{id}/items/{itemId}", cartId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void removeItem_DataCorrect_Returns200WithUpdatedCart() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(cartService.removeItem(cartId, itemId)).thenReturn(emptyCart(cartId));

        mockMvc.perform(delete("/carts/{id}/items/{itemId}", cartId, itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void removeItem_ItemNotFound_Returns404() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(cartService.removeItem(cartId, itemId)).thenThrow(new CartItemNotFoundException(itemId));

        mockMvc.perform(delete("/carts/{id}/items/{itemId}", cartId, itemId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void addItem_MissingIdsAndZeroQuantity_Returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/carts/{id}/items", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.startsWith("Validation failed: "),
                        org.hamcrest.Matchers.containsString("productId"),
                        org.hamcrest.Matchers.containsString("variantId"),
                        org.hamcrest.Matchers.containsString("quantity"))));
        org.mockito.Mockito.verifyNoInteractions(cartService);
    }

    @Test
    void addItem_AddOnWithZeroQuantity_Returns400() throws Exception {
        String body = """
                {"productId":"%s","variantId":"%s","quantity":2,
                 "addOns":[{"productId":"%s","quantity":0}]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/carts/{id}/items", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("addOns[0].quantity")));
        org.mockito.Mockito.verifyNoInteractions(cartService);
    }

    @Test
    void addItem_MalformedJson_Returns400() throws Exception {
        mockMvc.perform(post("/carts/{id}/items", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void changeItemQuantity_ZeroQuantity_Returns400WithoutCallingService() throws Exception {
        mockMvc.perform(patch("/carts/{id}/items/{itemId}", UUID.randomUUID(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed: quantity: must be greater than 0"));
        org.mockito.Mockito.verifyNoInteractions(cartService);
    }

    @Test
    void checkout_DataCorrect_Returns200WithCheckedOutCart() throws Exception {
        UUID cartId = UUID.randomUUID();
        Cart cart = cartWithItem(cartId);
        cart.setStatus(CartStatus.CHECKED_OUT);
        when(cartService.checkout(cartId, 1)).thenReturn(cart);

        mockMvc.perform(post("/carts/{id}/checkout", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.items[0].stockKeepingUnit").value("PBX15-I7-16"));
    }

    @Test
    void checkout_EmptyCart_Returns409() throws Exception {
        UUID cartId = UUID.randomUUID();
        when(cartService.checkout(cartId, 0)).thenThrow(new EmptyCartException(cartId));

        mockMvc.perform(post("/carts/{id}/checkout", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cart with id " + cartId + " is empty and cannot be checked out"));
    }
}
