package com.hungng3011.vdtecomberefresh.order.mappers;

import com.hungng3011.vdtecomberefresh.order.dtos.OrderItemDto;
import com.hungng3011.vdtecomberefresh.order.entities.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderItemMapper {

    @Mapping(source = "order.id", target = "orderId")
        // Map from OrderItem.order.id to OrderItemDto.orderId - now a String
    OrderItemDto toDto(OrderItem orderItem);

    List<OrderItemDto> toDtoList(List<OrderItem> orderItems);

    @Mapping(target = "order", ignore = true)
        // The Order reference is set by the OrderService, not by the mapper from DTO
    OrderItem toEntity(OrderItemDto orderItemDto);

    List<OrderItem> toEntityList(List<OrderItemDto> orderItemDtos);
}