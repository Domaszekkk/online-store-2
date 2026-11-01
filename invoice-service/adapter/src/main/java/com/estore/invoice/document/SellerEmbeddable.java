package com.estore.invoice.document;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SellerEmbeddable {

    @Column(name = "seller_name", length = 255)
    private String name;

    @Column(name = "seller_tax_identification_number", length = 10)
    private String taxIdentificationNumber;

    @Column(name = "seller_street", length = 255)
    private String street;

    @Column(name = "seller_city", length = 100)
    private String city;

    @Column(name = "seller_postal_code", length = 20)
    private String postalCode;

    @Column(name = "seller_country", length = 100)
    private String country;

    @Column(name = "seller_bank_account_number", length = 50)
    private String bankAccountNumber;
}
