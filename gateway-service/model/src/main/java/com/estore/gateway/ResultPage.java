package com.estore.gateway;

import java.util.List;

public record ResultPage<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
}
