package com.estore.order;

public class InvalidPageException extends OrderException {
    private InvalidPageException(String message) {
        super(message, 400);
    }

    public static InvalidPageException negativePage(int page) {
        return new InvalidPageException("Page must be 0 or greater, but was " + page);
    }

    public static InvalidPageException sizeOutOfRange(int size) {
        return new InvalidPageException("Page size must be between 1 and " + PageQuery.MAXIMUM_SIZE + ", but was " + size);
    }
}
