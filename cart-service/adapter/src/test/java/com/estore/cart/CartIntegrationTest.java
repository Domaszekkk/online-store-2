package com.estore.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ProductCatalogPort productCatalogPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String createCart() throws Exception {
        String response = mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
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
    void addItem_thenGetCart_persistsItemAndTotal() throws Exception {
        String cartId = createCart();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(productCatalogPort.getProduct(productId)).thenReturn(product(productId, variantId, "AVAILABLE"));

        String body = "{\"productId\":\"%s\",\"variantId\":\"%s\",\"quantity\":2}".formatted(productId, variantId);

        mockMvc.perform(post("/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].stockKeepingUnit").value("PBX15-I7-16"))
                .andExpect(jsonPath("$.total.amount").value(9198.00));

        mockMvc.perform(get("/carts/{id}", cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].variantId").value(variantId.toString()));
    }

    @Test
    void addItem_VariantNotAvailable_Returns409() throws Exception {
        String cartId = createCart();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(productCatalogPort.getProduct(productId)).thenReturn(product(productId, variantId, "COMING_SOON"));

        AddItemRequest request = AddItemRequest.builder()
                .productId(productId).variantId(variantId).quantity(1).build();

        mockMvc.perform(post("/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getCartById_CartNotFound_Returns404() throws Exception {
        mockMvc.perform(get("/carts/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void addSameItemTwice_thenChangeQuantity_thenRemove_persistsEachStep() throws Exception {
        String cartId = createCart();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(productCatalogPort.getProduct(productId)).thenReturn(product(productId, variantId, "AVAILABLE"));
        String body = "{\"productId\":\"%s\",\"variantId\":\"%s\",\"quantity\":1}".formatted(productId, variantId);

        mockMvc.perform(post("/carts/{id}/items", cartId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        String response = mockMvc.perform(post("/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andReturn().getResponse().getContentAsString();
        String itemId = objectMapper.readTree(response).get("items").get(0).get("id").asText();

        mockMvc.perform(patch("/carts/{id}/items/{itemId}", cartId, itemId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":3}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/carts/{id}", cartId))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.total.amount").value(13797.00));

        mockMvc.perform(delete("/carts/{id}/items/{itemId}", cartId, itemId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/carts/{id}", cartId))
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total.amount").value(0));
    }

    @Test
    void addTwoLaptopsWithOneCable_persistsAddOnQuantity() throws Exception {
        String cartId = createCart();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID cableId = UUID.randomUUID();
        ProductDetails laptop = product(productId, variantId, "AVAILABLE");
        laptop.setAddOns(List.of(AddOnDetails.builder()
                .productId(cableId).name("Kabel HDMI 2m")
                .price(Money.of(new BigDecimal("39.00"), "PLN")).build()));
        when(productCatalogPort.getProduct(productId)).thenReturn(laptop);
        String body = """
                {"productId":"%s","variantId":"%s","quantity":2,
                 "addOns":[{"productId":"%s","quantity":1}]}
                """.formatted(productId, variantId, cableId);

        mockMvc.perform(post("/carts/{id}/items", cartId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/carts/{id}", cartId))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].addOns[0].quantity").value(1))
                .andExpect(jsonPath("$.items[0].lineTotal.amount").value(9237.00))
                .andExpect(jsonPath("$.total.amount").value(9237.00));
    }

    @Test
    void addItem_InvalidPayload_Returns400() throws Exception {
        String cartId = createCart();

        mockMvc.perform(post("/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Validation failed: ")));
    }

    @Test
    void checkout_thenModifyOrCheckoutAgain_Returns409() throws Exception {
        String cartId = createCart();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(productCatalogPort.getProduct(productId)).thenReturn(product(productId, variantId, "AVAILABLE"));
        String body = "{\"productId\":\"%s\",\"variantId\":\"%s\",\"quantity\":1}".formatted(productId, variantId);

        mockMvc.perform(post("/carts/{id}/checkout", cartId))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/carts/{id}/items", cartId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/carts/{id}/checkout", cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.items.length()").value(1));

        mockMvc.perform(get("/carts/{id}", cartId))
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"));
        mockMvc.perform(post("/carts/{id}/items", cartId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/carts/{id}/checkout", cartId))
                .andExpect(status().isConflict());
    }
}
