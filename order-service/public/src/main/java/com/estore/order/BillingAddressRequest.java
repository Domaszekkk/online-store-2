package com.estore.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@CompleteCompanyData
public class BillingAddressRequest {
    @Size(max = 255)
    private String companyName;

    @ValidTaxIdentificationNumber
    private String taxIdentificationNumber;

    @NotBlank
    @Size(max = 255)
    private String street;

    @NotBlank
    @Size(max = 100)
    private String city;

    @NotBlank
    @Size(max = 20)
    private String postalCode;

    @NotBlank
    @Size(max = 100)
    private String country;
}
