package com.hungng3011.vdtecomberefresh.order.mappers;

import com.hungng3011.vdtecomberefresh.order.dtos.OrderItemDto;
import com.hungng3011.vdtecomberefresh.order.entities.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderItemMapper {

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.images", target = "productImage", qualifiedByName = "getFirstImage")
    OrderItemDto toDto(OrderItem orderItem);

    List<OrderItemDto> toDtoList(List<OrderItem> orderItems);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true) // Product will be set by service
    OrderItem toEntity(OrderItemDto orderItemDto);

    List<OrderItem> toEntityList(List<OrderItemDto> orderItemDtos);

    @Named("getFirstImage")
    default String getFirstImage(List<String> images) {
        return images != null && !images.isEmpty() ? images.get(0) : "";
    }
}