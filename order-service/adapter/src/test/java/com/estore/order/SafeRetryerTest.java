package com.estore.order;

import com.estore.order.client.SafeRetryer;
import feign.Request;
import feign.RetryableException;
import feign.Retryer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;

class SafeRetryerTest {

    private final Retryer retryer = new SafeRetryer(new Retryer.Default(1, 1, 3));

    private static RetryableException failure(Request.HttpMethod method, IOException cause) {
        Request request = Request.create(method, "http://cart-service/carts/1/checkout",
                Map.of(), null, StandardCharsets.UTF_8, null);
        return new RetryableException(-1, "failed", method, cause, (Long) null, request);
    }

    @Test
    void postRefusedConnection_IsRetried() {
        assertDoesNotThrow(() -> retryer.continueOrPropagate(
                failure(Request.HttpMethod.POST, new ConnectException("Connection refused"))));
    }

    @Test
    void postReadTimeout_IsNotRetried() {
        RetryableException exception = failure(Request.HttpMethod.POST, new SocketTimeoutException("Read timed out"));

        RetryableException thrown = Assertions.assertThrows(RetryableException.class,
                () -> retryer.continueOrPropagate(exception));

        assertSame(exception, thrown);
    }

    @Test
    void getReadTimeout_IsRetriedUntilMaxAttempts() {
        RetryableException exception = failure(Request.HttpMethod.GET, new SocketTimeoutException("Read timed out"));
        Retryer attempt = retryer.clone();

        assertDoesNotThrow(() -> attempt.continueOrPropagate(exception));
        assertDoesNotThrow(() -> attempt.continueOrPropagate(exception));
        Assertions.assertThrows(RetryableException.class, () -> attempt.continueOrPropagate(exception));
    }
}
