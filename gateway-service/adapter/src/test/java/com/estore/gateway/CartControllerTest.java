package com.estore.gateway;

import com.estore.gateway.cart.CartController;
import com.estore.gateway.cart.CartMapperImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    private ShopService shopService;

    private static Money pln(String amount) {
        return Money.of(new BigDecimal(amount), "PLN");
    }

    private static Cart sampleCart(UUID cartId) {
        return Cart.builder()
                .id(cartId)
                .status("OPEN")
                .items(List.of(CartItem.builder()
                        .id(UUID.randomUUID()).productId(UUID.randomUUID()).variantId(UUID.randomUUID())
                        .productName("ProBook X15").unitPrice(pln("4599.00")).quantity(2)
                        .addOns(List.of(CartItemAddOn.builder()
                                .productId(UUID.randomUUID()).name("Kabel HDMI 2m").price(pln("39.00")).quantity(1).build()))
                        .lineTotal(pln("9237.00"))
                        .build()))
                .total(pln("9237.00"))
                .build();
    }

    @Test
    void createCart_Returns201() throws Exception {
        UUID cartId = UUID.randomUUID();
        when(shopService.createCart()).thenReturn(Cart.builder().id(cartId).status("OPEN").items(List.of()).total(pln("0.00")).build());

        mockMvc.perform(post("/shop/carts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void addItem_DataCorrect_Returns200AndPassesSelection() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID addOnProductId = UUID.randomUUID();
        when(shopService.addItem(eq(cartId), any(AddItemCommand.class))).thenReturn(sampleCart(cartId));

        mockMvc.perform(post("/shop/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","variantId":"%s","quantity":2,
                                 "addOns":[{"productId":"%s","quantity":1}]}
                                """.formatted(productId, variantId, addOnProductId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].addOns[0].name").value("Kabel HDMI 2m"))
                .andExpect(jsonPath("$.items[0].stockKeepingUnit").doesNotExist())
                .andExpect(jsonPath("$.total.amount").value(9237.00));

        ArgumentCaptor<AddItemCommand> command = ArgumentCaptor.forClass(AddItemCommand.class);
        verify(shopService).addItem(eq(cartId), command.capture());
        assertAll(
                () -> assertEquals(productId, command.getValue().getProductId()),
                () -> assertEquals(variantId, command.getValue().getVariantId()),
                () -> assertEquals(2, command.getValue().getQuantity()),
                () -> assertEquals(addOnProductId, command.getValue().getAddOns().get(0).getProductId())
        );
    }

    @Test
    void addItem_MissingProductAndVariant_Returns400() throws Exception {
        mockMvc.perform(post("/shop/carts/{id}/items", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validation failed: productId: must not be null; variantId: must not be null"));
        verifyNoInteractions(shopService);
    }

    @Test
    void addItem_VariantUnavailable_Returns409WithServiceMessage() throws Exception {
        UUID cartId = UUID.randomUUID();
        when(shopService.addItem(eq(cartId), any(AddItemCommand.class)))
                .thenThrow(new ServiceRejectedRequestException("Variant is not available", 409));

        mockMvc.perform(post("/shop/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"%s\",\"variantId\":\"%s\",\"quantity\":1}"
                                .formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Variant is not available"));
    }

    @Test
    void changeItemQuantity_DataCorrect_Returns200() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(shopService.changeItemQuantity(cartId, itemId, 3)).thenReturn(sampleCart(cartId));

        mockMvc.perform(patch("/shop/carts/{id}/items/{itemId}", cartId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId.toString()));
        verify(shopService).changeItemQuantity(cartId, itemId, 3);
    }

    @Test
    void removeItem_DataCorrect_Returns200() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(shopService.removeItem(cartId, itemId)).thenReturn(sampleCart(cartId));

        mockMvc.perform(delete("/shop/carts/{id}/items/{itemId}", cartId, itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId.toString()));
    }
}
