package com.estore.gateway.order;

import com.estore.gateway.CheckoutOptions;
import com.estore.gateway.CheckoutOptionsDto;
import com.estore.gateway.DeliveryOption;
import com.estore.gateway.Order;
import com.estore.gateway.OrderDto;
import com.estore.gateway.OrderPreview;
import com.estore.gateway.OrderPreviewDto;
import com.estore.gateway.PlaceOrderCommand;
import com.estore.gateway.PlaceOrderRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface OrderMapper {

    Order toModel(OrderResponse response);

    OrderDto toDto(Order order);

    OrderPreview toPreview(OrderPreviewResponse response);

    OrderPreviewDto toDto(OrderPreview preview);

    PlaceOrderCommand toCommand(PlaceOrderRequest request);

    PlaceOrderPayload toPayload(PlaceOrderCommand command);

    List<DeliveryOption> toDeliveryOptions(List<DeliveryMethodResponse> responses);

    CheckoutOptionsDto toDto(CheckoutOptions checkoutOptions);
}
