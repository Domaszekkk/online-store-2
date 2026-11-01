package com.estore.gateway;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BillingAddress {
    private String companyName;
    private String taxIdentificationNumber;
    private String street;
    private String city;
    private String postalCode;
    private String country;
}
