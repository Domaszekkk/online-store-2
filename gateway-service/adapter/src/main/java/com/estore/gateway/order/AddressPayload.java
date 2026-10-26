package com.estore.gateway.order;

public record AddressPayload(String street, String city, String postalCode, String country) {
}
