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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    @Autowired
    private ProductRepositoryPort productRepositoryPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private AddProductCommand accessoryCommand() {
        return AddProductCommand.builder()
                .name("Mysz bezprzewodowa")
                .type(ProductType.ELECTRONICS)
                .basePrice(Money.of(new BigDecimal("99.00"), "PLN"))
                .build();
    }

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
                        .vatRate(VatRate.VAT_23)
                        .build()))
                .addOns(List.of(AddOn.builder()
                        .productId(accessoryId)
                        .name("Mysz")
                        .price(Money.of(new BigDecimal("99.00"), "PLN"))
                        .vatRate(VatRate.VAT_8)
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
                .andExpect(jsonPath("$.variants[0].vatRate").value("VAT_23"))
                .andExpect(jsonPath("$.addOns[0].name").value("Mysz"))
                .andExpect(jsonPath("$.addOns[0].vatRate").value("VAT_8"))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(created).get("id").asText();
        assertEquals(0, new BigDecimal("3999.00")
                .compareTo(new BigDecimal(objectMapper.readTree(created).get("basePrice").get("amount").asText())));

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("ProBook X15"))
                .andExpect(jsonPath("$.variants[0].stockKeepingUnit").value("PBX15-I7-16"))
                .andExpect(jsonPath("$.variants[0].optionValues.Procesor").value("i7"))
                .andExpect(jsonPath("$.variants[0].vatRate").value("VAT_23"))
                .andExpect(jsonPath("$.addOns[0].vatRate").value("VAT_8"));
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

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ProBook X15 Gen2"))
                .andExpect(jsonPath("$.attributes.screen").value("15.6"))
                .andExpect(jsonPath("$.options.length()").value(1))
                .andExpect(jsonPath("$.variants.length()").value(1))
                .andExpect(jsonPath("$.addOns.length()").value(1));
    }

    @Test
    void deleteProduct_ArchivesProductAndHidesItFromSale() throws Exception {
        String id = createProduct(sampleLaptopCommand());

        mockMvc.perform(delete("/products/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]").doesNotExist());
        mockMvc.perform(put("/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ProBook X15\",\"type\":\"COMPUTER\",\"basePrice\":{\"amount\":3999.00,\"currency\":\"PLN\"}}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/products/{id}", id))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/products/archived/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ProBook X15"))
                .andExpect(jsonPath("$.variants[0].stockKeepingUnit").value("PBX15-I7-16"))
                .andExpect(jsonPath("$.archivedAt").exists());
        mockMvc.perform(get("/products/archived"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists());
    }

    @Test
    void getArchivedProductById_ProductOnSale_Returns404() throws Exception {
        String id = createProduct(accessoryCommand());

        mockMvc.perform(get("/products/archived/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_ProductIsAddOnOfAnotherProduct_RemovesItFromThatProductAddOns() throws Exception {
        String laptopId = createProduct(sampleLaptopCommand());
        String accessoryId = objectMapper.readTree(
                        mockMvc.perform(get("/products/{id}", laptopId)).andReturn().getResponse().getContentAsString())
                .get("addOns").get(0).get("productId").asText();

        mockMvc.perform(delete("/products/{id}", accessoryId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/products/{id}", laptopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addOns.length()").value(0));
    }

    @Test
    void addProduct_AddOnProductArchived_Returns400() throws Exception {
        String accessoryId = createProduct(accessoryCommand());
        mockMvc.perform(delete("/products/{id}", accessoryId))
                .andExpect(status().isNoContent());
        String body = """
                {"name":"ProBook X15","type":"COMPUTER","basePrice":{"amount":3999.00,"currency":"PLN"},
                 "addOns":[{"productId":"%s","name":"Mysz","price":{"amount":99.00,"currency":"PLN"},"vatRate":"VAT_23"}]}
                """.formatted(accessoryId);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Add-on product with id " + accessoryId + " does not exist"));
    }

    @Test
    void deleteArchivedBefore_DeletesOnlyProductsArchivedBeforeCutoff() throws Exception {
        String archivedId = createProduct(sampleLaptopCommand());
        String activeId = createProduct(accessoryCommand());
        mockMvc.perform(delete("/products/{id}", archivedId))
                .andExpect(status().isNoContent());

        productRepositoryPort.deleteArchivedBefore(Instant.now().minus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/products/archived/{id}", archivedId))
                .andExpect(status().isOk());

        productRepositoryPort.deleteArchivedBefore(Instant.now().plus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/products/archived/{id}", archivedId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/products/{id}", activeId))
                .andExpect(status().isOk());
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
                 "addOns":[{"productId":"%s","name":"Mysz","price":{"amount":99.00,"currency":"PLN"},"vatRate":"VAT_23"}]}
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
                    "price":{"amount":4499.00,"currency":"PLN"},"availability":"AVAILABLE","vatRate":"VAT_23"},
                   {"stockKeepingUnit":"PBX15-I9-32","optionValues":{"Procesor":"i9"},
                    "price":{"amount":5799.00,"currency":"PLN"},"availability":"COMING_SOON","vatRate":"VAT_5"}
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
                .andExpect(jsonPath("$.variants[?(@.stockKeepingUnit=='PBX15-I9-32')].vatRate")
                        .value(org.hamcrest.Matchers.hasItem("VAT_5")))
                .andExpect(jsonPath("$.addOns.length()").value(0))
                .andExpect(jsonPath("$.options.length()").value(1));
    }
}
