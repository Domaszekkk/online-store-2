package com.estore.gateway;

import com.estore.gateway.product.ProductController;
import com.estore.gateway.product.ProductMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(ProductMapperImpl.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ShopService shopService;

    private static Money pln(String amount) {
        return Money.of(new BigDecimal(amount), "PLN");
    }

    @Test
    void getProducts_ByType_Returns200WithSummaries() throws Exception {
        UUID id = UUID.randomUUID();
        when(shopService.getProducts("COMPUTER")).thenReturn(List.of(ProductSummary.builder()
                .id(id).name("ProBook X15").type("COMPUTER").basePrice(pln("4599.00")).build()));

        mockMvc.perform(get("/shop/products").param("type", "COMPUTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("ProBook X15"))
                .andExpect(jsonPath("$[0].basePrice.amount").value(4599.00));
    }

    @Test
    void getProduct_DataCorrect_Returns200WithoutInternalFields() throws Exception {
        UUID id = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(shopService.getProduct(id)).thenReturn(ProductDetails.builder()
                .id(id).name("ProBook X15").type("COMPUTER").description("Laptop")
                .attributes(Map.of("brand", "ProBook"))
                .options(List.of(ProductOption.builder().name("RAM").values(List.of("16 GB", "32 GB")).build()))
                .variants(List.of(ProductVariant.builder()
                        .id(variantId).optionValues(Map.of("RAM", "16 GB")).price(pln("4599.00")).availability("AVAILABLE").build()))
                .addOns(List.of(ProductAddOn.builder().productId(UUID.randomUUID()).name("Kabel HDMI 2m").price(pln("39.00")).build()))
                .build());

        mockMvc.perform(get("/shop/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].values[1]").value("32 GB"))
                .andExpect(jsonPath("$.variants[0].id").value(variantId.toString()))
                .andExpect(jsonPath("$.variants[0].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.variants[0].vatRate").doesNotExist())
                .andExpect(jsonPath("$.variants[0].stockKeepingUnit").doesNotExist())
                .andExpect(jsonPath("$.addOns[0].name").value("Kabel HDMI 2m"))
                .andExpect(jsonPath("$.addOns[0].vatRate").doesNotExist());
    }

    @Test
    void getProduct_ProductNotFound_Returns404WithServiceMessage() throws Exception {
        UUID id = UUID.randomUUID();
        when(shopService.getProduct(id))
                .thenThrow(new ServiceRejectedRequestException("Product with id " + id + " not found", 404));

        mockMvc.perform(get("/shop/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product with id " + id + " not found"));
    }

    @Test
    void getProducts_ProductServiceDown_Returns503() throws Exception {
        when(shopService.getProducts(null)).thenThrow(new ServiceUnavailableException("product-service"));

        mockMvc.perform(get("/shop/products"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("Service product-service is currently unavailable, please try again later"));
    }

    @Test
    void preflight_AllowedOrigin_ReturnsCorsHeaders() throws Exception {
        mockMvc.perform(options("/shop/products")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void preflight_UnknownOrigin_Returns403() throws Exception {
        mockMvc.perform(options("/shop/products")
                        .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
