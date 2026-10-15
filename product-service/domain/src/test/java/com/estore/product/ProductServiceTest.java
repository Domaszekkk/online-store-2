package com.estore.product;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductRepositoryPort productRepositoryPort;
    private ProductService productService;

    @BeforeEach
    void setup() {
        this.productRepositoryPort = Mockito.mock(ProductRepositoryPort.class);
        this.productService = new ProductService(productRepositoryPort);
    }

    @Test
    void addProduct_DataCorrect_ReturnsProduct() {
        AddProductCommand command = AddProductCommand.builder()
                .name("ProBook X15")
                .type(ProductType.COMPUTER)
                .basePrice(Money.of(new BigDecimal("3999.00"), "PLN"))
                .build();
        when(productRepositoryPort.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.addProduct(command);

        assertAll(
                () -> assertNotNull(result.getId()),
                () -> assertEquals("ProBook X15", result.getName()),
                () -> assertEquals(ProductType.COMPUTER, result.getType()),
                () -> assertEquals(new BigDecimal("3999.00"), result.getBasePrice().amount()),
                () -> assertEquals("PLN", result.getBasePrice().currency())
        );
    }

    @Test
    void getProductById_DataCorrect_ReturnsProduct() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder().id(id).name("ProBook X15").type(ProductType.COMPUTER).build();
        when(productRepositoryPort.findById(id)).thenReturn(Optional.of(product));

        Product result = productService.getProductById(id);

        assertAll(
                () -> assertEquals(id, result.getId()),
                () -> assertEquals("ProBook X15", result.getName())
        );
    }

    @Test
    void getProductById_ProductNotFound_ThrowsProductNotFoundException() {
        UUID id = UUID.randomUUID();
        when(productRepositoryPort.findById(id)).thenReturn(Optional.empty());

        ProductNotFoundException exception = Assertions.assertThrows(
                ProductNotFoundException.class, () -> productService.getProductById(id));

        assertAll(
                () -> assertEquals("Product with id " + id + " not found", exception.getMessage()),
                () -> assertEquals(404, exception.getResponseCode())
        );
    }

    @Test
    void getProducts_ByType_ReturnsProducts() {
        Product product = Product.builder().id(UUID.randomUUID()).name("ProBook X15").type(ProductType.COMPUTER).build();
        when(productRepositoryPort.findAll(ProductType.COMPUTER)).thenReturn(List.of(product));

        List<Product> result = productService.getProducts(ProductType.COMPUTER);

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals(ProductType.COMPUTER, result.get(0).getType())
        );
    }

    @Test
    void getProducts_NoType_ReturnsAllProducts() {
        Product product1 = Product.builder().id(UUID.randomUUID()).name("ProBook X15").type(ProductType.COMPUTER).build();
        Product product2 = Product.builder().id(UUID.randomUUID()).name("Cable HDMI").type(ProductType.ELECTRONICS).build();
        when(productRepositoryPort.findAll(null)).thenReturn(List.of(product1, product2));

        List<Product> result = productService.getProducts(null);

        assertEquals(2, result.size());
    }

    @Test
    void getProductConfiguration_DataCorrect_ReturnsOptions() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder()
                .id(id)
                .name("ProBook X15")
                .type(ProductType.COMPUTER)
                .options(List.of(ProductOption.builder().name("Procesor").position(1).values(List.of("i5", "i7")).build()))
                .build();
        when(productRepositoryPort.findById(id)).thenReturn(Optional.of(product));

        List<ProductOption> result = productService.getProductConfiguration(id);

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals("Procesor", result.get(0).getName()),
                () -> assertEquals(2, result.get(0).getValues().size())
        );
    }

    @Test
    void updateProduct_DataCorrect_ReturnsUpdatedProduct() {
        UUID id = UUID.randomUUID();
        Product existing = Product.builder()
                .id(id).name("Old name").type(ProductType.COMPUTER)
                .basePrice(Money.of(new BigDecimal("3999.00"), "PLN"))
                .build();
        UpdateProductCommand request = UpdateProductCommand.builder()
                .name("New name").type(ProductType.COMPUTER).description("Updated")
                .basePrice(Money.of(new BigDecimal("4999.00"), "PLN"))
                .build();
        when(productRepositoryPort.findById(id)).thenReturn(Optional.of(existing));
        when(productRepositoryPort.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProduct(id, request);

        assertAll(
                () -> assertEquals(id, result.getId()),
                () -> assertEquals("New name", result.getName()),
                () -> assertEquals("Updated", result.getDescription()),
                () -> assertEquals(new BigDecimal("4999.00"), result.getBasePrice().amount())
        );
    }

    @Test
    void updateProduct_ProductNotFound_ThrowsProductNotFoundException() {
        UUID id = UUID.randomUUID();
        UpdateProductCommand request = UpdateProductCommand.builder().name("New name").build();
        when(productRepositoryPort.findById(id)).thenReturn(Optional.empty());

        ProductNotFoundException exception = Assertions.assertThrows(
                ProductNotFoundException.class, () -> productService.updateProduct(id, request));

        assertEquals(404, exception.getResponseCode());
        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void deleteProduct_DataCorrect_DeletesProduct() {
        UUID id = UUID.randomUUID();
        when(productRepositoryPort.existsById(id)).thenReturn(true);

        productService.deleteProduct(id);

        verify(productRepositoryPort).existsById(id);
        verify(productRepositoryPort).deleteById(id);
        verifyNoMoreInteractions(productRepositoryPort);
    }

    @Test
    void deleteProduct_ProductNotFound_ThrowsProductNotFoundException() {
        UUID id = UUID.randomUUID();
        when(productRepositoryPort.existsById(id)).thenReturn(false);

        ProductNotFoundException exception = Assertions.assertThrows(
                ProductNotFoundException.class, () -> productService.deleteProduct(id));

        assertEquals(404, exception.getResponseCode());
        verify(productRepositoryPort, never()).deleteById(any());
    }

    private AddOn addOn(UUID addOnProductId) {
        return AddOn.builder()
                .productId(addOnProductId).name("Mysz")
                .price(Money.of(new BigDecimal("99.00"), "PLN")).build();
    }

    private ProductVariant variant(UUID id, String stockKeepingUnit) {
        return ProductVariant.builder()
                .id(id).stockKeepingUnit(stockKeepingUnit)
                .price(Money.of(new BigDecimal("4599.00"), "PLN"))
                .availability(Availability.AVAILABLE).build();
    }

    @Test
    void addProduct_AddOnProductExists_SavesProduct() {
        UUID addOnProductId = UUID.randomUUID();
        AddProductCommand command = AddProductCommand.builder()
                .name("ProBook X15").type(ProductType.COMPUTER)
                .addOns(List.of(addOn(addOnProductId))).build();
        when(productRepositoryPort.existsById(addOnProductId)).thenReturn(true);
        when(productRepositoryPort.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.addProduct(command);

        assertEquals(addOnProductId, result.getAddOns().get(0).getProductId());
    }

    @Test
    void addProduct_AddOnProductDoesNotExist_ThrowsInvalidAddOnException() {
        UUID addOnProductId = UUID.randomUUID();
        AddProductCommand command = AddProductCommand.builder()
                .name("ProBook X15").type(ProductType.COMPUTER)
                .addOns(List.of(addOn(addOnProductId))).build();
        when(productRepositoryPort.existsById(addOnProductId)).thenReturn(false);

        InvalidAddOnException exception = Assertions.assertThrows(
                InvalidAddOnException.class, () -> productService.addProduct(command));

        assertAll(
                () -> assertEquals(400, exception.getResponseCode()),
                () -> assertEquals("Add-on product with id " + addOnProductId + " does not exist", exception.getMessage())
        );
        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void addProduct_AddOnWithoutProductId_ThrowsInvalidAddOnException() {
        AddProductCommand command = AddProductCommand.builder()
                .name("ProBook X15").type(ProductType.COMPUTER)
                .addOns(List.of(addOn(null))).build();

        InvalidAddOnException exception = Assertions.assertThrows(
                InvalidAddOnException.class, () -> productService.addProduct(command));

        assertEquals(400, exception.getResponseCode());
        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void updateProduct_WithVariants_KeepsExistingVariantIdsAndAssignsNewOnes() {
        UUID id = UUID.randomUUID();
        UUID existingVariantId = UUID.randomUUID();
        Product existing = Product.builder()
                .id(id).name("ProBook X15").type(ProductType.COMPUTER)
                .variants(new ArrayList<>(List.of(variant(existingVariantId, "PBX15-I7-16"))))
                .build();
        UpdateProductCommand request = UpdateProductCommand.builder()
                .name("ProBook X15").type(ProductType.COMPUTER)
                .variants(List.of(variant(existingVariantId, "PBX15-I7-16"), variant(null, "PBX15-I9-32")))
                .build();
        when(productRepositoryPort.findById(id)).thenReturn(Optional.of(existing));
        when(productRepositoryPort.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProduct(id, request);

        assertAll(
                () -> assertEquals(2, result.getVariants().size()),
                () -> assertEquals(existingVariantId, result.getVariants().get(0).getId()),
                () -> assertNotNull(result.getVariants().get(1).getId())
        );
    }

    @Test
    void updateProduct_CollectionsNotProvided_KeepsExistingCollections() {
        UUID id = UUID.randomUUID();
        Product existing = Product.builder()
                .id(id).name("ProBook X15").type(ProductType.COMPUTER)
                .options(List.of(ProductOption.builder().name("RAM").position(1).values(List.of("16GB")).build()))
                .variants(List.of(variant(UUID.randomUUID(), "PBX15-I7-16")))
                .build();
        UpdateProductCommand request = UpdateProductCommand.builder()
                .name("ProBook X15 Gen2").type(ProductType.COMPUTER).build();
        when(productRepositoryPort.findById(id)).thenReturn(Optional.of(existing));
        when(productRepositoryPort.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.updateProduct(id, request);

        assertAll(
                () -> assertEquals("ProBook X15 Gen2", result.getName()),
                () -> assertEquals(1, result.getOptions().size()),
                () -> assertEquals(1, result.getVariants().size())
        );
    }

    @Test
    void updateProduct_AddOnReferencesSameProduct_ThrowsInvalidAddOnException() {
        UUID id = UUID.randomUUID();
        Product existing = Product.builder().id(id).name("ProBook X15").type(ProductType.COMPUTER).build();
        UpdateProductCommand request = UpdateProductCommand.builder()
                .name("ProBook X15").type(ProductType.COMPUTER)
                .addOns(List.of(addOn(id))).build();
        when(productRepositoryPort.findById(id)).thenReturn(Optional.of(existing));

        InvalidAddOnException exception = Assertions.assertThrows(
                InvalidAddOnException.class, () -> productService.updateProduct(id, request));

        assertEquals(400, exception.getResponseCode());
        verify(productRepositoryPort, never()).save(any());
    }
}
