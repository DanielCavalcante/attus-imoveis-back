package com.example.demo.dtos;
import java.math.BigDecimal;

import com.example.demo.enums.PropertyType;
import com.example.demo.enums.ReasonType;

public record AnnouncementDetailDTO(
    Long id,
    String title,
    String description,
    String city,
    String state,
    String street,
    String image,
    Integer streetNumber,
    String cep,
    String complement,
    PropertyType propertyType,
    ReasonType reason,
    Integer rooms,
    Integer bathRooms,
    BigDecimal area,
    BigDecimal price
) {}
