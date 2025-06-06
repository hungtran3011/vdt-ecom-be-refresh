package com.hungng3011.vdtecomberefresh.cart.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.hungng3011.vdtecomberefresh.cart.dtos.CartItemDto;
import com.hungng3011.vdtecomberefresh.cart.entities.CartItem;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartItemMapper {
    public CartItemDto toDto(CartItem cartItem);
    
    @Mapping(target = "cart", ignore = true)
    @Mapping(target = "product", ignore = true)
    public CartItem toEntity(CartItemDto cartItemDto);
}
