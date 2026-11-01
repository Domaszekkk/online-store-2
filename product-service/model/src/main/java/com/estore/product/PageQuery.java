package com.estore.product;

public record PageQuery(int page, int size) {
    public static final int MAXIMUM_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw InvalidPageException.negativePage(page);
        }
        if (size < 1 || size > MAXIMUM_SIZE) {
            throw InvalidPageException.sizeOutOfRange(size);
        }
    }
}
