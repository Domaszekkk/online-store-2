package com.estore.invoice.event;

public record BillingAddressPayload(String companyName,
                                    String taxIdentificationNumber,
                                    String street,
                                    String city,
                                    String postalCode,
                                    String country) {
}
