package com.estore.invoice;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SellerDto {
    private String name;
    private String taxIdentificationNumber;
    private String street;
    private String city;
    private String postalCode;
    private String country;
    private String bankAccountNumber;
}
