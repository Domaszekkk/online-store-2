package com.estore.gateway.order;

public record CustomerPayload(String firstName, String lastName, String email, String phone) {
}
