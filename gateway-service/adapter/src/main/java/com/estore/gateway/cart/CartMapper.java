package com.estore.gateway.cart;

import com.estore.gateway.AddItemCommand;
import com.estore.gateway.AddItemRequest;
import com.estore.gateway.Cart;
import com.estore.gateway.CartDto;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface CartMapper {

    Cart toModel(CartResponse response);

    CartDto toDto(Cart cart);

    AddItemCommand toCommand(AddItemRequest request);

    AddItemPayload toPayload(AddItemCommand command);
}
