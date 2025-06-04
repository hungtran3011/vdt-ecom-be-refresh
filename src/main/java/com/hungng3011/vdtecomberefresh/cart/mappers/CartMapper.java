package com.hungng3011.vdtecomberefresh.cart.mappers;

import com.hungng3011.vdtecomberefresh.cart.dtos.CartDto;
import com.hungng3011.vdtecomberefresh.cart.entities.Cart;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CartMapper {
    public CartDto toDto(Cart cart);
    public Cart toEntity(CartDto cartDto);
}
