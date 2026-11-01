package com.estore.cart.client;

import com.estore.cart.AddOnDetails;
import com.estore.cart.Money;
import com.estore.cart.ProductCatalogPort;
import com.estore.cart.ProductCatalogUnavailableException;
import com.estore.cart.ProductDetails;
import com.estore.cart.ProductNotFoundException;
import com.estore.cart.VariantDetails;
import com.estore.cart.VatRate;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCatalogAdapter implements ProductCatalogPort {
    private final ProductClient productClient;

    @Override
    public ProductDetails getProduct(UUID productId) {
        return findProduct(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Override
    public Optional<ProductDetails> findProduct(UUID productId) {
        try {
            return Optional.of(map(productClient.getProduct(productId)));
        } catch (FeignException.NotFound exception) {
            return Optional.empty();
        } catch (FeignException exception) {
            log.warn("Product service call failed for product {} (status {})", productId, exception.status(), exception);
            throw new ProductCatalogUnavailableException();
        }
    }

    private ProductDetails map(ProductResponse response) {
        List<VariantDetails> variants = (response.variants() == null ? List.<VariantResponse>of() : response.variants())
                .stream()
                .map(variant -> VariantDetails.builder()
                        .id(variant.id())
                        .stockKeepingUnit(variant.stockKeepingUnit())
                        .price(money(variant.price()))
                        .availability(variant.availability())
                        .vatRate(vatRate(variant.vatRate()))
                        .build())
                .toList();

        List<AddOnDetails> addOns = (response.addOns() == null ? List.<AddOnResponse>of() : response.addOns())
                .stream()
                .map(addOn -> AddOnDetails.builder()
                        .productId(addOn.productId())
                        .name(addOn.name())
                        .price(money(addOn.price()))
                        .vatRate(vatRate(addOn.vatRate()))
                        .availability(addOn.availability())
                        .build())
                .toList();

        return ProductDetails.builder()
                .id(response.id())
                .name(response.name())
                .variants(variants)
                .addOns(addOns)
                .build();
    }

    private VatRate vatRate(String value) {
        if (value == null) {
            throw new IllegalStateException("Product service returned a price without a VAT rate");
        }
        return VatRate.valueOf(value);
    }

    private Money money(MoneyPayload payload) {
        return payload == null ? null : Money.of(payload.amount(), payload.currency());
    }
}
