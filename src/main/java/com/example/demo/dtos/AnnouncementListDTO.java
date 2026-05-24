package com.example.demo.dtos;
import java.math.BigDecimal;

import com.example.demo.enums.PropertyType;
import com.example.demo.enums.ReasonType;

public record AnnouncementListDTO(
    Long id,
    String title,
    String city,
    String state,
    String image,
    PropertyType propertyType,
    ReasonType reason,
    Integer rooms,
    Integer bathRooms,
    BigDecimal area,
    BigDecimal price
) {}
