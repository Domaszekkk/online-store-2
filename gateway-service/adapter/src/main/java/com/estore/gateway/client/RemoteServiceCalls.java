package com.estore.gateway.client;

import com.estore.gateway.GatewayException;
import com.estore.gateway.ServiceRejectedRequestException;
import com.estore.gateway.ServiceUnavailableException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteServiceCalls {
    private final JsonMapper jsonMapper;

    public <T> T call(String serviceName, Supplier<T> call) {
        try {
            return call.get();
        } catch (FeignException exception) {
            throw translate(serviceName, exception);
        }
    }

    public GatewayException translate(String serviceName, FeignException exception) {
        int status = exception.status();
        if (status >= 400 && status < 500) {
            return new ServiceRejectedRequestException(messageFrom(serviceName, exception), status);
        }
        log.warn("Call to {} failed (status {})", serviceName, status, exception);
        return new ServiceUnavailableException(serviceName);
    }

    private String messageFrom(String serviceName, FeignException exception) {
        try {
            ErrorResponse error = jsonMapper.readValue(exception.contentUTF8(), ErrorResponse.class);
            if (error != null && error.message() != null) {
                return error.message();
            }
        } catch (JacksonException ignored) {
        }
        return "Request rejected by " + serviceName + " with status " + exception.status();
    }
}
