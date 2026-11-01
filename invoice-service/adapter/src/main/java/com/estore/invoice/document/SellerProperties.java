package com.estore.invoice.document;

import com.estore.invoice.Seller;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "invoice.seller")
public record SellerProperties(
        String name,
        String taxIdentificationNumber,
        String street,
        String city,
        String postalCode,
        String country,
        String bankAccountNumber
) {
    public Seller toSeller() {
        return Seller.builder()
                .name(name)
                .taxIdentificationNumber(taxIdentificationNumber)
                .street(street)
                .city(city)
                .postalCode(postalCode)
                .country(country)
                .bankAccountNumber(bankAccountNumber)
                .build();
    }
}
