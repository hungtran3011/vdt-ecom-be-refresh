package com.hungng3011.vdtecomberefresh.cart.mappers;

import com.hungng3011.vdtecomberefresh.cart.dtos.CartDto;
import com.hungng3011.vdtecomberefresh.cart.entities.Cart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {
    public CartDto toDto(Cart cart);
    
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "sessionId", ignore = true)
    public Cart toEntity(CartDto cartDto);
}
