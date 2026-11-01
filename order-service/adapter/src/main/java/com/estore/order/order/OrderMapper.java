package com.estore.order.order;

import com.estore.order.DeliveryMethod;
import com.estore.order.DeliveryMethodDto;
import com.estore.order.Money;
import com.estore.order.MoneyDto;
import com.estore.order.Order;
import com.estore.order.OrderDto;
import com.estore.order.OrderPreviewDto;
import com.estore.order.PageDto;
import com.estore.order.ResultPage;
import com.estore.order.PaymentMethod;
import com.estore.order.PlaceOrderCommand;
import com.estore.order.PlaceOrderRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface OrderMapper {

    OrderDto toDto(Order order);

    OrderPreviewDto toPreviewDto(Order order);

    List<OrderDto> toDtos(List<Order> orders);

    default PageDto<OrderDto> toPageDto(ResultPage<Order> page) {
        return PageDto.<OrderDto>builder()
                .items(toDtos(page.items()))
                .page(page.page())
                .size(page.size())
                .totalItems(page.totalItems())
                .totalPages(page.totalPages())
                .build();
    }

    OrderEntity toEntity(Order order);

    @Mapping(target = "cartVersion", ignore = true)
    Order toModel(OrderEntity entity);

    PlaceOrderCommand toCommand(PlaceOrderRequest request);

    MoneyDto toMoneyDto(Money money);

    List<DeliveryMethodDto> toDeliveryMethodDtos(List<DeliveryMethod> deliveryMethods);

    default DeliveryMethodDto toDeliveryMethodDto(DeliveryMethod deliveryMethod) {
        return DeliveryMethodDto.builder()
                .method(deliveryMethod.name())
                .cost(toMoneyDto(deliveryMethod.cost()))
                .build();
    }

    List<String> toPaymentMethodNames(List<PaymentMethod> paymentMethods);
}
