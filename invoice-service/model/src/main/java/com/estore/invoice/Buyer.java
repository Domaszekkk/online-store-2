package com.estore.invoice;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Buyer {
    private String fullName;
    private String companyName;
    private String taxIdentificationNumber;
    private String street;
    private String city;
    private String postalCode;
    private String country;
    private String email;

    public boolean isCompany() {
        return taxIdentificationNumber != null && !taxIdentificationNumber.isBlank();
    }
}
