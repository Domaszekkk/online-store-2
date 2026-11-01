package com.estore.product;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

    private ProductRepositoryPort productRepositoryPort;
    private ProductService productService;

    @BeforeEach
    void setup() {
        this.productRepositoryPort = Mockito.mock(ProductRepositoryPort.class);
        TransactionPort transactionPort = Mockito.mock(TransactionPort.class);
        when(transactionPort.inTransaction(any())).thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
        when(productRepositoryPort.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        this.productService = new ProductService(
                productRepositoryPort, transactionPort, Clock.fixed(NOW, ZoneOffset.UTC), Period.ofYears(2));
    }

    private AddOn addOn(UUID addOnProductId) {
        return AddOn.builder().productId(addOnProductId).build();
    }

    private ProductVariant variant(UUID id, String stockKeepingUnit, Map<String, String> optionValues) {
        return ProductVariant.builder()
                .id(id).stockKeepingUnit(stockKeepingUnit)
                .optionValues(optionValues)
                .price(Money.of(new BigDecimal("4599.00"), "PLN"))
                .availability(Availability.AVAILABLE).vatRate(VatRate.VAT_23).build();
    }

    private ProductOption processorOption() {
        return ProductOption.builder().name("Procesor").position(1).values(List.of("i5", "i7", "i9")).build();
    }

    private AddProductCommand.AddProductCommandBuilder laptopCommand() {
        return AddProductCommand.builder()
                .name("ProBook X15")
                .type(ProductType.COMPUTER)
                .basePrice(Money.of(new BigDecimal("3999.00"), "PLN"))
                .options(new ArrayList<>(List.of(processorOption())))
                .variants(new ArrayList<>(List.of(variant(null, "PBX15-I7", Map.of("Procesor", "i7")))));
    }

    private AddProductCommand.AddProductCommandBuilder cableCommand() {
        return AddProductCommand.builder()
                .name("Kabel HDMI")
                .type(ProductType.ELECTRONICS)
                .basePrice(Money.of(new BigDecimal("39.00"), "PLN"))
                .variants(new ArrayList<>(List.of(variant(null, "HDMI-2M", Map.of()))));
    }

    private Product existingLaptop(UUID id, UUID variantId) {
        return Product.builder()
                .id(id).name("ProBook X15").type(ProductType.COMPUTER)
                .basePrice(Money.of(new BigDecimal("3999.00"), "PLN"))
                .options(new ArrayList<>(List.of(processorOption())))
                .variants(new ArrayList<>(List.of(variant(variantId, "PBX15-I7", Map.of("Procesor", "i7")))))
                .build();
    }

    private Product existingCable(UUID id, Availability availability) {
        ProductVariant variant = variant(UUID.randomUUID(), "HDMI-2M", Map.of());
        variant.setAvailability(availability);
        return Product.builder()
                .id(id).name("Kabel HDMI").type(ProductType.ELECTRONICS)
                .basePrice(Money.of(new BigDecimal("39.00"), "PLN"))
                .variants(new ArrayList<>(List.of(variant)))
                .build();
    }

    @Test
    void addProduct_DataCorrect_ReturnsProduct() {
        Product result = productService.addProduct(laptopCommand().build());

        assertAll(
                () -> assertNotNull(result.getId()),
                () -> assertEquals("ProBook X15", result.getName()),
                () -> assertEquals(ProductType.COMPUTER, result.getType()),
                () -> assertEquals(new BigDecimal("3999.00"), result.getBasePrice().amount()),
                () -> assertNotNull(result.getVariants().get(0).getId())
        );
    }

    @Test
    void addProduct_VariantIdGivenByClient_AssignsNewVariantId() {
        UUID foreignVariantId = UUID.randomUUID();
        AddProductCommand command = laptopCommand()
                .variants(new ArrayList<>(List.of(variant(foreignVariantId, "PBX15-I7", Map.of("Procesor", "i7")))))
                .build();

        Product result = productService.addProduct(command);

        assertNotEquals(foreignVariantId, result.getVariants().get(0).getId());
    }

    @Test
    void addProduct_NoVariants_ThrowsInvalidProductException() {
        AddProductCommand command = laptopCommand().variants(new ArrayList<>()).build();

        InvalidProductException exception = Assertions.assertThrows(
                InvalidProductException.class, () -> productService.addProduct(command));

        assertAll(
                () -> assertEquals(400, exception.getResponseCode()),
                () -> assertEquals("Product must have at least one variant", exception.getMessage())
        );
        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void addProduct_ElectronicsWithOptions_ThrowsInvalidProductException() {
        AddProductCommand command = cableCommand().options(List.of(processorOption())).build();

        InvalidProductException exception = Assertions.assertThrows(
                InvalidProductException.class, () -> productService.addProduct(command));

        assertEquals("Product of type ELECTRONICS cannot have configurable options", exception.getMessage());
    }

    @Test
    void addProduct_ElectronicsWithTwoVariants_ThrowsInvalidProductException() {
        AddProductCommand command = cableCommand()
                .variants(new ArrayList<>(List.of(variant(null, "HDMI-2M", Map.of()), variant(null, "HDMI-3M", Map.of()))))
                .build();

        InvalidProductException exception = Assertions.assertThrows(
                InvalidProductException.class, () -> productService.addProduct(command));

        assertEquals("Product of type ELECTRONICS must have exactly one variant, but has 2", exception.getMessage());
    }

    @Test
    void addProduct_ElectronicsWithAddOns_ThrowsInvalidProductException() {
        AddProductCommand command = cableCommand().addOns(List.of(addOn(UUID.randomUUID()))).build();

        InvalidProductException exception = Assertions.assertThrows(
                InvalidProductException.class, () -> productService.addProduct(command));

        assertEquals("Product of type ELECTRONICS cannot have add-ons", exception.getMessage());
    }

    @Test
    void addProduct_VariantValueOutsideOption_ThrowsInvalidProductException() {
        AddProductCommand command = laptopCommand()
                .variants(new ArrayList<>(List.of(variant(null, "PBX15-M1", Map.of("Procesor", "M1")))))
                .build();

        InvalidProductException exception = Assertions.assertThrows(
                InvalidProductException.class, () -> productService.addProduct(command));

        assertEquals("Variant PBX15-M1 has value M1 that is not allowed for option Procesor", exception.getMessage());
    }

    @Test
    void addProduct_VariantWithUnknownOption_ThrowsInvalidProductException() {
        AddProductCommand command = laptopCommand()
                .variants(new ArrayList<>(List.of(variant(null, "PBX15-I7", Map.of("Procesor", "i7", "Kolor", "Czarny")))))
                .build();

        InvalidProductException exception = Assertions.assertThrows(
                InvalidProductException.class, () -> productService.addProduct(command));

        assertEquals("Variant PBX15-I7 must have exactly one value for each option [Procesor]", exception.getMessage());
    }

    @Test
    void addProduct_TwoVariantsWithSameOptionValues_ThrowsInvalidProductException() {
        AddProductCommand command = laptopCommand()
                .variants(new ArrayList<>(List.of(
                        variant(null, "PBX15-I7", Map.of("Procesor", "i7")),
                        variant(null, "PBX15-I7-B", Map.of("Procesor", "i7")))))
                .build();

        InvalidProductException exception = Assertions.assertThrows(
                InvalidProductException.class, () -> productService.addProduct(command));

        assertEquals("Variant PBX15-I7-B repeats the option values of another variant", exception.getMessage());
    }

    @Test
    void addProduct_StockKeepingUnitUsedByOtherProduct_ThrowsStockKeepingUnitTakenException() {
        when(productRepositoryPort.findStockKeepingUnitsUsedByOtherActiveProducts(anyCollection(), any(UUID.class)))
                .thenReturn(List.of("PBX15-I7"));

        StockKeepingUnitTakenException exception = Assertions.assertThrows(
                StockKeepingUnitTakenException.class, () -> productService.addProduct(laptopCommand().build()));

        assertAll(
                () -> assertEquals(409, exception.getResponseCode()),
                () -> assertEquals("Stock keeping units [PBX15-I7] are already used by another product on sale",
                        exception.getMessage())
        );
        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void addProduct_StockKeepingUnitsChecked_LocksThemBeforeChecking() {
        productService.addProduct(laptopCommand().build());

        InOrder inOrder = Mockito.inOrder(productRepositoryPort);
        inOrder.verify(productRepositoryPort).lockStockKeepingUnits(Set.of("PBX15-I7"));
        inOrder.verify(productRepositoryPort).findStockKeepingUnitsUsedByOtherActiveProducts(eq(Set.of("PBX15-I7")), any(UUID.class));
        inOrder.verify(productRepositoryPort).save(any(Product.class));
    }

    @Test
    void addProduct_AddOnProductExists_SavesProductWithAddOnAvailability() {
        UUID addOnProductId = UUID.randomUUID();
        Product cable = existingCable(addOnProductId, Availability.AVAILABLE);
        when(productRepositoryPort.findActiveById(addOnProductId)).thenReturn(Optional.of(cable));
        when(productRepositoryPort.findAllActiveByIds(Set.of(addOnProductId))).thenReturn(List.of(cable));

        Product result = productService.addProduct(laptopCommand().addOns(List.of(addOn(addOnProductId))).build());

        assertAll(
                () -> assertEquals(addOnProductId, result.getAddOns().get(0).getProductId()),
                () -> assertEquals(Availability.AVAILABLE, result.getAddOns().get(0).getAvailability())
        );
    }

    @Test
    void addProduct_AddOnIsNotElectronics_ThrowsInvalidAddOnException() {
        UUID addOnProductId = UUID.randomUUID();
        when(productRepositoryPort.findActiveById(addOnProductId))
                .thenReturn(Optional.of(existingLaptop(addOnProductId, UUID.randomUUID())));

        InvalidAddOnException exception = Assertions.assertThrows(InvalidAddOnException.class,
                () -> productService.addProduct(laptopCommand().addOns(List.of(addOn(addOnProductId))).build()));

        assertEquals("Add-on product " + addOnProductId + " is of type COMPUTER, but only ELECTRONICS products can be sold as add-ons",
                exception.getMessage());
        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void addProduct_SameAddOnTwice_ThrowsInvalidAddOnException() {
        UUID addOnProductId = UUID.randomUUID();
        when(productRepositoryPort.findActiveById(addOnProductId))
                .thenReturn(Optional.of(existingCable(addOnProductId, Availability.AVAILABLE)));

        InvalidAddOnException exception = Assertions.assertThrows(InvalidAddOnException.class,
                () -> productService.addProduct(laptopCommand()
                        .addOns(List.of(addOn(addOnProductId), addOn(addOnProductId))).build()));

        assertEquals("Add-on product " + addOnProductId + " is listed more than once", exception.getMessage());
    }

    @Test
    void getProductById_AddOnSoldOut_ReturnsAddOnAsSoldOut() {
        UUID id = UUID.randomUUID();
        UUID addOnProductId = UUID.randomUUID();
        Product laptop = existingLaptop(id, UUID.randomUUID());
        laptop.setAddOns(new ArrayList<>(List.of(addOn(addOnProductId))));
        when(productRepositoryPort.findActiveById(id)).thenReturn(Optional.of(laptop));
        when(productRepositoryPort.findAllActiveByIds(Set.of(addOnProductId)))
                .thenReturn(List.of(existingCable(addOnProductId, Availability.SOLD_OUT)));

        Product result = productService.getProductById(id);

        AddOn addOn = result.getAddOns().get(0);
        assertAll(
                () -> assertEquals(Availability.SOLD_OUT, addOn.getAvailability()),
                () -> assertEquals("Kabel HDMI", addOn.getName()),
                () -> assertEquals(new BigDecimal("4599.00"), addOn.getPrice().amount()),
                () -> assertEquals(VatRate.VAT_23, addOn.getVatRate())
        );
    }

    @Test
    void getProductById_AccessoryNoLongerOnSale_LeavesAddOnOut() {
        UUID id = UUID.randomUUID();
        UUID addOnProductId = UUID.randomUUID();
        Product laptop = existingLaptop(id, UUID.randomUUID());
        laptop.setAddOns(new ArrayList<>(List.of(addOn(addOnProductId))));
        when(productRepositoryPort.findActiveById(id)).thenReturn(Optional.of(laptop));
        when(productRepositoryPort.findAllActiveByIds(Set.of(addOnProductId))).thenReturn(List.of());

        Product result = productService.getProductById(id);

        assertEquals(0, result.getAddOns().size());
    }

    @Test
    void addProduct_AddOnProductDoesNotExist_ThrowsInvalidAddOnException() {
        UUID addOnProductId = UUID.randomUUID();
        when(productRepositoryPort.findActiveById(addOnProductId)).thenReturn(Optional.empty());

        InvalidAddOnException exception = Assertions.assertThrows(InvalidAddOnException.class,
                () -> productService.addProduct(laptopCommand().addOns(List.of(addOn(addOnProductId))).build()));

        assertAll(
                () -> assertEquals(400, exception.getResponseCode()),
                () -> assertEquals("Add-on product with id " + addOnProductId + " does not exist", exception.getMessage())
        );
        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void addProduct_AddOnWithoutProductId_ThrowsInvalidAddOnException() {
        InvalidAddOnException exception = Assertions.assertThrows(InvalidAddOnException.class,
                () -> productService.addProduct(laptopCommand().addOns(List.of(addOn(null))).build()));

        assertEquals(400, exception.getResponseCode());
        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void getProductById_DataCorrect_ReturnsProduct() {
        UUID id = UUID.randomUUID();
        when(productRepositoryPort.findActiveById(id)).thenReturn(Optional.of(existingLaptop(id, UUID.randomUUID())));

        Product result = productService.getProductById(id);

        assertAll(
                () -> assertEquals(id, result.getId()),
                () -> assertEquals("ProBook X15", result.getName())
        );
    }

    @Test
    void getProductById_ProductNotFound_ThrowsProductNotFoundException() {
        UUID id = UUID.randomUUID();
        when(productRepositoryPort.findActiveById(id)).thenReturn(Optional.empty());

        ProductNotFoundException exception = Assertions.assertThrows(
                ProductNotFoundException.class, () -> productService.getProductById(id));

        assertAll(
                () -> assertEquals("Product with id " + id + " not found", exception.getMessage()),
                () -> assertEquals(404, exception.getResponseCode())
        );
    }

    @Test
    void getProducts_ByType_ReturnsPageOfProducts() {
        PageQuery pageQuery = new PageQuery(0, 20);
        when(productRepositoryPort.findAllActive(ProductType.COMPUTER, pageQuery))
                .thenReturn(new ResultPage<>(List.of(existingLaptop(UUID.randomUUID(), UUID.randomUUID())), 0, 20, 1));

        ResultPage<Product> result = productService.getProducts(ProductType.COMPUTER, pageQuery);

        assertAll(
                () -> assertEquals(1, result.items().size()),
                () -> assertEquals(ProductType.COMPUTER, result.items().get(0).getType()),
                () -> assertEquals(1, result.totalPages())
        );
    }

    @Test
    void getProducts_NoType_ReturnsAllProductsOfThePage() {
        PageQuery pageQuery = new PageQuery(1, 2);
        Product cable = Product.builder().id(UUID.randomUUID()).name("Cable HDMI").type(ProductType.ELECTRONICS).build();
        when(productRepositoryPort.findAllActive(null, pageQuery))
                .thenReturn(new ResultPage<>(List.of(existingLaptop(UUID.randomUUID(), UUID.randomUUID()), cable), 1, 2, 5));

        ResultPage<Product> result = productService.getProducts(null, pageQuery);

        assertAll(
                () -> assertEquals(2, result.items().size()),
                () -> assertEquals(3, result.totalPages())
        );
    }

    @Test
    void pageQuery_SizeAboveLimit_ThrowsInvalidPageException() {
        InvalidPageException exception = Assertions.assertThrows(InvalidPageException.class, () -> new PageQuery(0, 101));

        assertAll(
                () -> assertEquals(400, exception.getResponseCode()),
                () -> assertEquals("Page size must be between 1 and 100, but was 101", exception.getMessage())
        );
    }

    @Test
    void pageQuery_NegativePage_ThrowsInvalidPageException() {
        InvalidPageException exception = Assertions.assertThrows(InvalidPageException.class, () -> new PageQuery(-1, 20));

        assertEquals("Page must be 0 or greater, but was -1", exception.getMessage());
    }

    @Test
    void getProductConfiguration_DataCorrect_ReturnsOptions() {
        UUID id = UUID.randomUUID();
        when(productRepositoryPort.findActiveById(id)).thenReturn(Optional.of(existingLaptop(id, UUID.randomUUID())));

        List<ProductOption> result = productService.getProductConfiguration(id);

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals("Procesor", result.get(0).getName()),
                () -> assertEquals(3, result.get(0).getValues().size())
        );
    }

    @Test
    void updateProduct_DataCorrect_ReturnsUpdatedProduct() {
        UUID id = UUID.randomUUID();
        UpdateProductCommand request = UpdateProductCommand.builder()
                .name("New name").type(ProductType.COMPUTER).description("Updated")
                .basePrice(Money.of(new BigDecimal("4999.00"), "PLN"))
                .build();
        when(productRepositoryPort.findActiveByIdForUpdate(id))
                .thenReturn(Optional.of(existingLaptop(id, UUID.randomUUID())));

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
        when(productRepositoryPort.findActiveByIdForUpdate(id)).thenReturn(Optional.empty());

        ProductNotFoundException exception = Assertions.assertThrows(
                ProductNotFoundException.class, () -> productService.updateProduct(id, request));

        assertEquals(404, exception.getResponseCode());
        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void updateProduct_WithVariants_KeepsExistingVariantIdsAndAssignsNewOnes() {
        UUID id = UUID.randomUUID();
        UUID existingVariantId = UUID.randomUUID();
        UpdateProductCommand request = UpdateProductCommand.builder()
                .name("ProBook X15").type(ProductType.COMPUTER)
                .variants(List.of(
                        variant(existingVariantId, "PBX15-I7", Map.of("Procesor", "i7")),
                        variant(null, "PBX15-I9", Map.of("Procesor", "i9"))))
                .build();
        when(productRepositoryPort.findActiveByIdForUpdate(id)).thenReturn(Optional.of(existingLaptop(id, existingVariantId)));

        Product result = productService.updateProduct(id, request);

        assertAll(
                () -> assertEquals(2, result.getVariants().size()),
                () -> assertEquals(existingVariantId, result.getVariants().get(0).getId()),
                () -> assertNotNull(result.getVariants().get(1).getId())
        );
    }

    @Test
    void updateProduct_VariantOfAnotherProduct_ThrowsInvalidProductException() {
        UUID id = UUID.randomUUID();
        UUID foreignVariantId = UUID.randomUUID();
        UpdateProductCommand request = UpdateProductCommand.builder()
                .name("ProBook X15").type(ProductType.COMPUTER)
                .variants(List.of(variant(foreignVariantId, "PBX15-I7", Map.of("Procesor", "i7"))))
                .build();
        when(productRepositoryPort.findActiveByIdForUpdate(id))
                .thenReturn(Optional.of(existingLaptop(id, UUID.randomUUID())));

        InvalidProductException exception = Assertions.assertThrows(
                InvalidProductException.class, () -> productService.updateProduct(id, request));

        assertEquals("Variant " + foreignVariantId + " does not belong to product " + id, exception.getMessage());
        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void updateProduct_SameVariantIdTwice_ThrowsInvalidProductException() {
        UUID id = UUID.randomUUID();
        UUID existingVariantId = UUID.randomUUID();
        UpdateProductCommand request = UpdateProductCommand.builder()
                .name("ProBook X15").type(ProductType.COMPUTER)
                .variants(List.of(
                        variant(existingVariantId, "PBX15-I7", Map.of("Procesor", "i7")),
                        variant(existingVariantId, "PBX15-I9", Map.of("Procesor", "i9"))))
                .build();
        when(productRepositoryPort.findActiveByIdForUpdate(id)).thenReturn(Optional.of(existingLaptop(id, existingVariantId)));

        InvalidProductException exception = Assertions.assertThrows(
                InvalidProductException.class, () -> productService.updateProduct(id, request));

        assertAll(
                () -> assertEquals(400, exception.getResponseCode()),
                () -> assertEquals("Variant " + existingVariantId + " is listed more than once", exception.getMessage())
        );
        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void updateProduct_AddOnOfOtherProductChangedToComputer_ThrowsInvalidProductException() {
        UUID id = UUID.randomUUID();
        UpdateProductCommand request = UpdateProductCommand.builder()
                .name("Kabel HDMI").type(ProductType.COMPUTER).build();
        when(productRepositoryPort.findActiveByIdForUpdate(id))
                .thenReturn(Optional.of(existingCable(id, Availability.AVAILABLE)));
        when(productRepositoryPort.isAddOnOfActiveProduct(id)).thenReturn(true);

        InvalidProductException exception = Assertions.assertThrows(
                InvalidProductException.class, () -> productService.updateProduct(id, request));

        assertEquals("Product " + id + " is an add-on of other products, so its type cannot change to COMPUTER; "
                + "only ELECTRONICS products can be sold as add-ons", exception.getMessage());
        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void updateProduct_StockKeepingUnitUsedByOtherProduct_ThrowsStockKeepingUnitTakenException() {
        UUID id = UUID.randomUUID();
        UpdateProductCommand request = UpdateProductCommand.builder()
                .name("ProBook X15").type(ProductType.COMPUTER)
                .variants(List.of(variant(null, "HDMI-2M", Map.of("Procesor", "i5"))))
                .build();
        when(productRepositoryPort.findActiveByIdForUpdate(id))
                .thenReturn(Optional.of(existingLaptop(id, UUID.randomUUID())));
        when(productRepositoryPort.findStockKeepingUnitsUsedByOtherActiveProducts(anyCollection(), eq(id)))
                .thenReturn(List.of("HDMI-2M"));

        Assertions.assertThrows(StockKeepingUnitTakenException.class, () -> productService.updateProduct(id, request));

        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void updateProduct_CollectionsNotProvided_KeepsExistingCollections() {
        UUID id = UUID.randomUUID();
        UpdateProductCommand request = UpdateProductCommand.builder()
                .name("ProBook X15 Gen2").type(ProductType.COMPUTER).build();
        when(productRepositoryPort.findActiveByIdForUpdate(id))
                .thenReturn(Optional.of(existingLaptop(id, UUID.randomUUID())));

        Product result = productService.updateProduct(id, request);

        assertAll(
                () -> assertEquals("ProBook X15 Gen2", result.getName()),
                () -> assertEquals(1, result.getOptions().size()),
                () -> assertEquals(1, result.getVariants().size())
        );
    }

    @Test
    void updateProduct_TypeChangedToElectronicsWithOptions_ThrowsInvalidProductException() {
        UUID id = UUID.randomUUID();
        UpdateProductCommand request = UpdateProductCommand.builder()
                .name("ProBook X15").type(ProductType.ELECTRONICS).build();
        when(productRepositoryPort.findActiveByIdForUpdate(id))
                .thenReturn(Optional.of(existingLaptop(id, UUID.randomUUID())));

        Assertions.assertThrows(InvalidProductException.class, () -> productService.updateProduct(id, request));

        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void updateProduct_AddOnReferencesSameProduct_ThrowsInvalidAddOnException() {
        UUID id = UUID.randomUUID();
        UpdateProductCommand request = UpdateProductCommand.builder()
                .name("ProBook X15").type(ProductType.COMPUTER)
                .addOns(List.of(addOn(id))).build();
        when(productRepositoryPort.findActiveByIdForUpdate(id))
                .thenReturn(Optional.of(existingLaptop(id, UUID.randomUUID())));

        InvalidAddOnException exception = Assertions.assertThrows(
                InvalidAddOnException.class, () -> productService.updateProduct(id, request));

        assertEquals(400, exception.getResponseCode());
        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void archiveProduct_ActiveProduct_SetsArchiveDateAndRemovesItFromOtherProductsAddOns() {
        UUID id = UUID.randomUUID();
        Product product = existingLaptop(id, UUID.randomUUID());
        when(productRepositoryPort.findActiveByIdForUpdate(id)).thenReturn(Optional.of(product));

        productService.archiveProduct(id);

        assertEquals(NOW, product.getArchivedAt());
        verify(productRepositoryPort).removeAddOnsReferencing(id);
        verify(productRepositoryPort).save(product);
    }

    @Test
    void archiveProduct_ProductNotFound_ThrowsProductNotFoundException() {
        UUID id = UUID.randomUUID();
        when(productRepositoryPort.findActiveByIdForUpdate(id)).thenReturn(Optional.empty());

        ProductNotFoundException exception = Assertions.assertThrows(
                ProductNotFoundException.class, () -> productService.archiveProduct(id));

        assertEquals(404, exception.getResponseCode());
        verify(productRepositoryPort, never()).removeAddOnsReferencing(any());
        verify(productRepositoryPort, never()).save(any());
    }

    @Test
    void getArchivedProductById_ProductArchived_ReturnsProduct() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder().id(id).name("Kabel HDMI").archivedAt(NOW).build();
        when(productRepositoryPort.findArchivedById(id)).thenReturn(Optional.of(product));

        Product result = productService.getArchivedProductById(id);

        assertAll(
                () -> assertEquals(id, result.getId()),
                () -> assertEquals(NOW, result.getArchivedAt())
        );
    }

    @Test
    void getArchivedProductById_ProductNotArchived_ThrowsProductNotFoundException() {
        UUID id = UUID.randomUUID();
        when(productRepositoryPort.findArchivedById(id)).thenReturn(Optional.empty());

        ProductNotFoundException exception = Assertions.assertThrows(
                ProductNotFoundException.class, () -> productService.getArchivedProductById(id));

        assertEquals(404, exception.getResponseCode());
    }

    @Test
    void deleteExpiredArchivedProducts_DeletesProductsArchivedBeforeRetentionPeriod() {
        when(productRepositoryPort.deleteArchivedBefore(Instant.parse("2024-10-22T10:00:00Z"))).thenReturn(3L);

        long result = productService.deleteExpiredArchivedProducts();

        assertEquals(3, result);
    }
}
