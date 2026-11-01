package com.estore.order;

import java.util.List;

public record ResultPage<T>(List<T> items, int page, int size, long totalItems) {

    public int totalPages() {
        return (int) ((totalItems + size - 1) / size);
    }
}
