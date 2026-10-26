package com.estore.invoice.event;

public record AddOnPayload(String name, MoneyPayload price, String vatRate, int quantity) {
}
