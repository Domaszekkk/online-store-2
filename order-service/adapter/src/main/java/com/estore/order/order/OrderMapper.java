package com.estore.order.order;

import com.estore.order.Order;
import com.estore.order.OrderDto;
import com.estore.order.PlaceOrderCommand;
import com.estore.order.PlaceOrderRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface OrderMapper {

    OrderDto toDto(Order order);

    List<OrderDto> toDtos(List<Order> orders);

    OrderEntity toEntity(Order order);

    Order toModel(OrderEntity entity);

    PlaceOrderCommand toCommand(PlaceOrderRequest request);
}
