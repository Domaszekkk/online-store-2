package com.estore.gateway.client;

import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

public class RetryOnServerErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        Exception exception = defaultDecoder.decode(methodKey, response);
        if (response.status() >= 500 && !(exception instanceof RetryableException)) {
            return new RetryableException(
                    response.status(),
                    exception.getMessage(),
                    response.request().httpMethod(),
                    exception,
                    (Long) null,
                    response.request());
        }
        return exception;
    }
}
