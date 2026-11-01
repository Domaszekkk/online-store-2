package com.estore.gateway.order;

public record BillingAddressPayload(String companyName,
                                    String taxIdentificationNumber,
                                    String street,
                                    String city,
                                    String postalCode,
                                    String country) {
}
