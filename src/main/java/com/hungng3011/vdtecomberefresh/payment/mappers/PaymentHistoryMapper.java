package com.hungng3011.vdtecomberefresh.payment.mappers;

import com.hungng3011.vdtecomberefresh.payment.dtos.PaymentHistoryDto;
import com.hungng3011.vdtecomberefresh.payment.entities.PaymentHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentHistoryMapper {
    
    PaymentHistoryDto toDto(PaymentHistory entity);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaymentHistory toEntity(PaymentHistoryDto dto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(PaymentHistoryDto dto, @MappingTarget PaymentHistory entity);
}
