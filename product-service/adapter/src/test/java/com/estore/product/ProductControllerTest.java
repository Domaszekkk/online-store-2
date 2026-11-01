package com.estore.product;

import com.estore.product.product.ProductController;
import com.estore.product.product.ProductMapperImpl;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(ProductMapperImpl.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ProductService productService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Product sampleProduct(UUID id) {
        return Product.builder()
                .id(id)
                .name("ProBook X15")
                .type(ProductType.COMPUTER)
                .description("Business laptop")
                .basePrice(Money.of(new BigDecimal("3999.00"), "PLN"))
                .options(List.of(ProductOption.builder().name("Procesor").position(1).values(List.of("i5", "i7")).build()))
                .variants(List.of(ProductVariant.builder()
                        .id(UUID.randomUUID())
                        .stockKeepingUnit("PBX15-I7-16")
                        .optionValues(Map.of("Procesor", "i7"))
                        .price(Money.of(new BigDecimal("4599.00"), "PLN"))
                        .availability(Availability.AVAILABLE)
                        .vatRate(VatRate.VAT_23)
                        .build()))
                .addOns(List.of(AddOn.builder()
                        .productId(UUID.randomUUID())
                        .name("Mysz")
                        .price(Money.of(new BigDecimal("99.00"), "PLN"))
                        .vatRate(VatRate.VAT_8)
                        .availability(Availability.AVAILABLE)
                        .build()))
                .build();
    }

    @Test
    void addProduct_DataCorrect_Returns201() throws Exception {
        UUID id = UUID.randomUUID();
        AddProductCommand command = AddProductCommand.builder()
                .name("ProBook X15").type(ProductType.COMPUTER)
                .basePrice(Money.of(new BigDecimal("3999.00"), "PLN"))
                .build();
        when(productService.addProduct(any(AddProductCommand.class))).thenReturn(sampleProduct(id));

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("ProBook X15"))
                .andExpect(jsonPath("$.type").value("COMPUTER"))
                .andExpect(jsonPath("$.variants[0].stockKeepingUnit").value("PBX15-I7-16"))
                .andExpect(jsonPath("$.variants[0].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.variants[0].vatRate").value("VAT_23"))
                .andExpect(jsonPath("$.addOns[0].name").value("Mysz"))
                .andExpect(jsonPath("$.addOns[0].vatRate").value("VAT_8"));
    }

    @Test
    void getProductById_DataCorrect_Returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getProductById(id)).thenReturn(sampleProduct(id));

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("ProBook X15"));
    }

    @Test
    void getProductById_ProductNotFound_Returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getProductById(id)).thenThrow(new ProductNotFoundException(id));

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product with id " + id + " not found"));
    }

    @Test
    void getProducts_ByType_Returns200WithFirstPageByDefault() throws Exception {
        when(productService.getProducts(ProductType.COMPUTER, new PageQuery(0, 20)))
                .thenReturn(new ResultPage<>(List.of(sampleProduct(UUID.randomUUID())), 0, 20, 21));

        mockMvc.perform(get("/products").param("type", "COMPUTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].type").value("COMPUTER"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(21))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getProducts_PageSizeAboveLimit_Returns400() throws Exception {
        mockMvc.perform(get("/products").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page size must be between 1 and 100, but was 101"));
    }

    @Test
    void getArchivedProducts_Returns200WithArchiveDate() throws Exception {
        Product product = sampleProduct(UUID.randomUUID());
        product.archive(Instant.parse("2026-10-22T10:00:00Z"));
        when(productService.getArchivedProducts(new PageQuery(1, 5))).thenReturn(new ResultPage<>(List.of(product), 1, 5, 6));

        mockMvc.perform(get("/products/archived").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("ProBook X15"))
                .andExpect(jsonPath("$.items[0].archivedAt").value("2026-10-22T10:00:00Z"))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void getArchivedProductById_DataCorrect_Returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Product product = sampleProduct(id);
        product.archive(Instant.parse("2026-10-22T10:00:00Z"));
        when(productService.getArchivedProductById(id)).thenReturn(product);

        mockMvc.perform(get("/products/archived/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.archivedAt").value("2026-10-22T10:00:00Z"));
    }

    @Test
    void getArchivedProductById_ProductNotArchived_Returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getArchivedProductById(id)).thenThrow(new ProductNotFoundException(id));

        mockMvc.perform(get("/products/archived/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product with id " + id + " not found"));
    }

    @Test
    void getProductConfiguration_Returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getProductConfiguration(id)).thenReturn(
                List.of(ProductOption.builder().name("Procesor").position(1).values(List.of("i5", "i7")).build()));

        mockMvc.perform(get("/products/{id}/configuration", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Procesor"))
                .andExpect(jsonPath("$[0].values.length()").value(2));
    }

    @Test
    void updateProduct_DataCorrect_Returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("ProBook X15 Gen2").type(ProductType.COMPUTER)
                .basePrice(new MoneyRequest(new BigDecimal("4299.00"), "PLN"))
                .build();
        when(productService.updateProduct(eq(id), any(UpdateProductCommand.class))).thenReturn(sampleProduct(id));

        mockMvc.perform(put("/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void deleteProduct_DataCorrect_ArchivesProductAndReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(productService).archiveProduct(id);

        mockMvc.perform(delete("/products/{id}", id))
                .andExpect(status().isNoContent());

        verify(productService).archiveProduct(id);
    }

    @Test
    void deleteProduct_ProductNotFound_Returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ProductNotFoundException(id)).when(productService).archiveProduct(id);

        mockMvc.perform(delete("/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product with id " + id + " not found"));
    }

    @Test
    void addProduct_AddOnProductDoesNotExist_Returns400() throws Exception {
        UUID missingProductId = UUID.randomUUID();
        AddProductCommand command = AddProductCommand.builder()
                .name("ProBook X15").type(ProductType.COMPUTER)
                .basePrice(Money.of(new BigDecimal("3999.00"), "PLN"))
                .build();
        when(productService.addProduct(any(AddProductCommand.class)))
                .thenThrow(InvalidAddOnException.productNotFound(missingProductId));

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Add-on product with id " + missingProductId + " does not exist"));
    }

    @Test
    void addProduct_EmptyBody_Returns400WithAllMissingFields() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Validation failed: basePrice: must not be null; name: must not be blank; type: must not be null"));
        org.mockito.Mockito.verifyNoInteractions(productService);
    }

    @Test
    void addProduct_InvalidNestedVariantAndMoney_Returns400() throws Exception {
        String body = """
                {"name":"ProBook X15","type":"COMPUTER","basePrice":{"amount":-1,"currency":"pln"},
                 "variants":[{"stockKeepingUnit":"","price":{"amount":4599.00,"currency":"PLN"}}]}
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("basePrice.amount"),
                        org.hamcrest.Matchers.containsString("basePrice.currency"),
                        org.hamcrest.Matchers.containsString("variants[0].stockKeepingUnit"),
                        org.hamcrest.Matchers.containsString("variants[0].availability"),
                        org.hamcrest.Matchers.containsString("variants[0].vatRate"))));
        org.mockito.Mockito.verifyNoInteractions(productService);
    }

    @Test
    void addProduct_UnknownProductType_Returns400() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Toster\",\"type\":\"TOASTER\",\"basePrice\":{\"amount\":99.00,\"currency\":\"PLN\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Malformed request body")));
        org.mockito.Mockito.verifyNoInteractions(productService);
    }

    @Test
    void addProduct_StockKeepingUnitTaken_Returns409() throws Exception {
        AddProductCommand command = AddProductCommand.builder()
                .name("ProBook X15").type(ProductType.COMPUTER)
                .basePrice(Money.of(new BigDecimal("3999.00"), "PLN"))
                .build();
        when(productService.addProduct(any(AddProductCommand.class)))
                .thenThrow(new StockKeepingUnitTakenException(List.of("PBX15-I7-16")));

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Stock keeping units [PBX15-I7-16] are already used by another product on sale"));
    }

    @Test
    void addProduct_ElectronicsWithOptions_Returns400() throws Exception {
        AddProductCommand command = AddProductCommand.builder()
                .name("Kabel HDMI").type(ProductType.ELECTRONICS)
                .basePrice(Money.of(new BigDecimal("39.00"), "PLN"))
                .build();
        when(productService.addProduct(any(AddProductCommand.class)))
                .thenThrow(InvalidProductException.electronicsWithOptions());

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product of type ELECTRONICS cannot have configurable options"));
    }
}
