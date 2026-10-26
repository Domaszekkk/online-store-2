package com.estore.order.event;

public record CustomerPayload(String firstName, String lastName, String email, String phone) {
}
