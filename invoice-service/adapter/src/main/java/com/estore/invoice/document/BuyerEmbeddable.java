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
public class BuyerEmbeddable {

    @Column(name = "buyer_full_name", length = 255)
    private String fullName;

    @Column(name = "buyer_company_name", length = 255)
    private String companyName;

    @Column(name = "buyer_tax_identification_number", length = 10)
    private String taxIdentificationNumber;

    @Column(name = "buyer_street", length = 255)
    private String street;

    @Column(name = "buyer_city", length = 100)
    private String city;

    @Column(name = "buyer_postal_code", length = 20)
    private String postalCode;

    @Column(name = "buyer_country", length = 100)
    private String country;

    @Column(name = "buyer_email", length = 255)
    private String email;
}
