package com.estore.gateway.product;

import java.util.List;

public record ProductOptionResponse(String name, List<String> values) {
}
