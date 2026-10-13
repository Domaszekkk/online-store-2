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
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private AddProductCommand sampleLaptopCommand() {
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
                        .productId(UUID.randomUUID())
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
    void getProductById_ProductNotFound_Returns404() throws Exception {
        mockMvc.perform(get("/products/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
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
                .basePrice(Money.of(new BigDecimal("4299.00"), "PLN"))
                .build();

        mockMvc.perform(put("/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ProBook X15 Gen2"))
                .andExpect(jsonPath("$.description").value("Updated laptop"));

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ProBook X15 Gen2"));
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
    void updateProduct_ProductNotFound_Returns404() throws Exception {
        UpdateProductRequest request = UpdateProductRequest.builder().name("Nope").build();

        mockMvc.perform(put("/products/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void deleteProduct_ProductNotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/products/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}
