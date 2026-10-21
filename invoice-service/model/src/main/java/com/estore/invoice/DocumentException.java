package com.estore.invoice;

import lombok.Getter;

@Getter
public class DocumentException extends RuntimeException {
    private final Integer responseCode;

    public DocumentException(String message, Integer responseCode) {
        super(message);
        this.responseCode = responseCode;
    }
}
