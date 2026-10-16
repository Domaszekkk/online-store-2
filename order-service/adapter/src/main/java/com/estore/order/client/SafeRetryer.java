package com.estore.order.client;

import feign.Request;
import feign.RetryableException;
import feign.Retryer;

import java.net.ConnectException;


public class SafeRetryer implements Retryer {
    private final Retryer delegate;

    public SafeRetryer(Retryer delegate) {
        this.delegate = delegate;
    }

    @Override
    public void continueOrPropagate(RetryableException exception) {
        if (!isSafeToRetry(exception)) {
            throw exception;
        }
        delegate.continueOrPropagate(exception);
    }

    private boolean isSafeToRetry(RetryableException exception) {
        return exception.method() == Request.HttpMethod.GET || exception.getCause() instanceof ConnectException;
    }

    @Override
    public Retryer clone() {
        return new SafeRetryer(delegate.clone());
    }
}
