package com.estore.gateway.product;

import java.util.List;

public record ProductPageResponse(List<ProductResponse> items, int page, int size, long totalItems, int totalPages) {
}
