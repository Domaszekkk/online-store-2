package com.estore.order.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BillingAddressEmbeddable {

    @Column(name = "billing_company_name", length = 255)
    private String companyName;

    @Column(name = "billing_tax_identification_number", length = 10)
    private String taxIdentificationNumber;

    @Column(name = "billing_street", length = 255)
    private String street;

    @Column(name = "billing_city", length = 100)
    private String city;

    @Column(name = "billing_postal_code", length = 20)
    private String postalCode;

    @Column(name = "billing_country", length = 100)
    private String country;
}
