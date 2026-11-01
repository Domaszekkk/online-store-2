package com.estore.gateway.client;

import feign.Request;
import feign.RetryableException;
import feign.Retryer;

public class GetOnlyRetryer implements Retryer {
    private final Retryer delegate;

    public GetOnlyRetryer(Retryer delegate) {
        this.delegate = delegate;
    }

    @Override
    public void continueOrPropagate(RetryableException exception) {
        if (exception.method() != Request.HttpMethod.GET) {
            throw exception;
        }
        delegate.continueOrPropagate(exception);
    }

    @Override
    public Retryer clone() {
        return new GetOnlyRetryer(delegate.clone());
    }
}
