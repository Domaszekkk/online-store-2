package com.estore.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private AddProductCommand accessoryCommand() {
        return AddProductCommand.builder()
                .name("Mysz bezprzewodowa")
                .type(ProductType.ELECTRONICS)
                .basePrice(Money.of(new BigDecimal("99.00"), "PLN"))
                .build();
    }

    /** Add-ons must reference existing products, so the accessory is created first. */
    private AddProductCommand sampleLaptopCommand() throws Exception {
        UUID accessoryId = UUID.fromString(createProduct(accessoryCommand()));
        return AddProductCommand.builder()
                .name("ProBook X15")
                .type(ProductType.COMPUTER)
                .description("Business laptop")
                .basePrice(Money.of(new BigDecimal("3999.00"), "PLN"))
                .attributes(Map.of("screen", "15.6"))
                .options(List.of(ProductOption.builder()
                        .name("Procesor")
                        .position(1)
                        .values(List.of("i5", "i7"))
                        .build()))
                .variants(List.of(ProductVariant.builder()
                        .stockKeepingUnit("PBX15-I7-16")
                        .optionValues(Map.of("Procesor", "i7"))
                        .price(Money.of(new BigDecimal("4599.00"), "PLN"))
                        .availability(Availability.AVAILABLE)
                        .build()))
                .addOns(List.of(AddOn.builder()
                        .productId(accessoryId)
                        .name("Mysz")
                        .price(Money.of(new BigDecimal("99.00"), "PLN"))
                        .build()))
                .build();
    }

    private String createProduct(AddProductCommand command) throws Exception {
        String response = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void addProduct_thenGetById_returnsPersistedProduct() throws Exception {
        AddProductCommand command = sampleLaptopCommand();

        String created = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("ProBook X15"))
                .andExpect(jsonPath("$.type").value("COMPUTER"))
                .andExpect(jsonPath("$.basePrice.currency").value("PLN"))
                .andExpect(jsonPath("$.options.length()").value(1))
                .andExpect(jsonPath("$.options[0].name").value("Procesor"))
                .andExpect(jsonPath("$.options[0].values.length()").value(2))
                .andExpect(jsonPath("$.variants.length()").value(1))
                .andExpect(jsonPath("$.variants[0].id").exists())
                .andExpect(jsonPath("$.variants[0].stockKeepingUnit").value("PBX15-I7-16"))
                .andExpect(jsonPath("$.variants[0].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.addOns[0].name").value("Mysz"))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(created).get("id").asText();
        assertEquals(0, new BigDecimal("3999.00")
                .compareTo(new BigDecimal(objectMapper.readTree(created).get("basePrice").get("amount").asText())));

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("ProBook X15"))
                .andExpect(jsonPath("$.variants[0].stockKeepingUnit").value("PBX15-I7-16"))
                .andExpect(jsonPath("$.variants[0].optionValues.Procesor").value("i7"));
    }

    @Test
    void getProducts_ByType_ReturnsMatchingProducts() throws Exception {
        String id = createProduct(sampleLaptopCommand());

        mockMvc.perform(get("/products").param("type", "COMPUTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].type").value(org.hamcrest.Matchers.hasItem("COMPUTER")));
    }

    @Test
    void getProducts_NoType_ReturnsAllProducts() throws Exception {
        String id = createProduct(sampleLaptopCommand());

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists());
    }

    @Test
    void getProductConfiguration_ReturnsOptions() throws Exception {
        String id = createProduct(sampleLaptopCommand());

        mockMvc.perform(get("/products/{id}/configuration", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Procesor"))
                .andExpect(jsonPath("$[0].values.length()").value(2));
    }

    @Test
    void updateProduct_ChangesDetails() throws Exception {
        String id = createProduct(sampleLaptopCommand());
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("ProBook X15 Gen2")
                .type(ProductType.COMPUTER)
                .description("Updated laptop")
                .basePrice(new MoneyRequest(new BigDecimal("4299.00"), "PLN"))
                .build();

        mockMvc.perform(put("/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ProBook X15 Gen2"))
                .andExpect(jsonPath("$.description").value("Updated laptop"));

        // collections not sent in the request must stay untouched
        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ProBook X15 Gen2"))
                .andExpect(jsonPath("$.attributes.screen").value("15.6"))
                .andExpect(jsonPath("$.options.length()").value(1))
                .andExpect(jsonPath("$.variants.length()").value(1))
                .andExpect(jsonPath("$.addOns.length()").value(1));
    }

    @Test
    void deleteProduct_RemovesProduct() throws Exception {
        String id = createProduct(sampleLaptopCommand());

        mockMvc.perform(delete("/products/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void addProduct_MinimalPayload_DefaultsEmptyCollections() throws Exception {
        String body = "{\"name\":\"Kabel HDMI\",\"type\":\"ELECTRONICS\",\"basePrice\":{\"amount\":39.00,\"currency\":\"PLN\"}}";

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Kabel HDMI"))
                .andExpect(jsonPath("$.options.length()").value(0))
                .andExpect(jsonPath("$.variants.length()").value(0))
                .andExpect(jsonPath("$.addOns.length()").value(0));
    }

    @Test
    void addProduct_AddOnProductDoesNotExist_Returns400() throws Exception {
        UUID missingProductId = UUID.randomUUID();
        String body = """
                {"name":"ProBook X15","type":"COMPUTER","basePrice":{"amount":3999.00,"currency":"PLN"},
                 "addOns":[{"productId":"%s","name":"Mysz","price":{"amount":99.00,"currency":"PLN"}}]}
                """.formatted(missingProductId);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Add-on product with id " + missingProductId + " does not exist"));
    }

    @Test
    void updateProduct_ReplacesVariantsAndKeepsExistingVariantId() throws Exception {
        String id = createProduct(sampleLaptopCommand());
        String existingVariantId = objectMapper.readTree(
                        mockMvc.perform(get("/products/{id}", id)).andReturn().getResponse().getContentAsString())
                .get("variants").get(0).get("id").asText();
        String body = """
                {"name":"ProBook X15","type":"COMPUTER","basePrice":{"amount":3999.00,"currency":"PLN"},
                 "variants":[
                   {"id":"%s","stockKeepingUnit":"PBX15-I7-16","optionValues":{"Procesor":"i7"},
                    "price":{"amount":4499.00,"currency":"PLN"},"availability":"AVAILABLE"},
                   {"stockKeepingUnit":"PBX15-I9-32","optionValues":{"Procesor":"i9"},
                    "price":{"amount":5799.00,"currency":"PLN"},"availability":"COMING_SOON"}
                 ],
                 "addOns":[]}
                """.formatted(existingVariantId);

        mockMvc.perform(put("/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants.length()").value(2))
                .andExpect(jsonPath("$.variants[?(@.stockKeepingUnit=='PBX15-I7-16')].id")
                        .value(org.hamcrest.Matchers.hasItem(existingVariantId)))
                .andExpect(jsonPath("$.variants[?(@.stockKeepingUnit=='PBX15-I7-16')].price.amount")
                        .value(org.hamcrest.Matchers.hasItem(4499.00)))
                .andExpect(jsonPath("$.addOns.length()").value(0))
                .andExpect(jsonPath("$.options.length()").value(1));
    }
}
