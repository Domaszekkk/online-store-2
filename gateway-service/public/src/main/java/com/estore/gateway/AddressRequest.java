package com.estore.gateway;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressRequest {
    private String street;
    private String city;
    private String postalCode;
    private String country;
}
