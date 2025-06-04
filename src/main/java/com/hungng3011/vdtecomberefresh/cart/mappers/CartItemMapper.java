package com.hungng3011.vdtecomberefresh.cart.mappers;

import org.mapstruct.Mapper;
import com.hungng3011.vdtecomberefresh.cart.dtos.CartItemDto;
import com.hungng3011.vdtecomberefresh.cart.entities.CartItem;

@Mapper(componentModel = "spring")
public interface CartItemMapper {
    public CartItemDto toDto(CartItem cartItem);
    public CartItem toEntity(CartItemDto cartItemDto);
}
