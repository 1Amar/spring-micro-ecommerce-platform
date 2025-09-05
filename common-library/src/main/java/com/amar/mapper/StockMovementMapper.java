package com.amar.mapper;

import com.amar.dto.StockMovementDto;
import com.amar.entity.inventory.StockMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StockMovementMapper {

    StockMovementMapper INSTANCE = Mappers.getMapper(StockMovementMapper.class);

    @Mapping(target = "movementType", expression = "java(stockMovement.getMovementType().toString())")
    StockMovementDto toDto(StockMovement stockMovement);

    @Mapping(target = "movementType", expression = "java(com.amar.entity.inventory.StockMovement.MovementType.valueOf(stockMovementDto.getMovementType()))")
    StockMovement toEntity(StockMovementDto stockMovementDto);

    List<StockMovementDto> toDtoList(List<StockMovement> stockMovements);

    List<StockMovement> toEntityList(List<StockMovementDto> stockMovementDtos);
}