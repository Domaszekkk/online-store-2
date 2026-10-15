package com.estore.product;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductException.class)
    public ResponseEntity<ErrorMessage> handleProductException(ProductException exception) {
        return ResponseEntity
                .status(exception.getResponseCode())
                .body(new ErrorMessage(exception.getMessage()));
    }
}
