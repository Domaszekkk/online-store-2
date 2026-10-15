package com.estore.cart.cart;

import com.estore.cart.AddItemCommand;
import com.estore.cart.AddItemRequest;
import com.estore.cart.Cart;
import com.estore.cart.CartDto;
import com.estore.cart.ChangeItemQuantityCommand;
import com.estore.cart.ChangeItemQuantityRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface CartMapper {

    CartDto toDto(Cart cart);

    CartEntity toEntity(Cart cart);

    Cart toModel(CartEntity entity);

    AddItemCommand toCommand(AddItemRequest request);

    ChangeItemQuantityCommand toCommand(ChangeItemQuantityRequest request);
}
