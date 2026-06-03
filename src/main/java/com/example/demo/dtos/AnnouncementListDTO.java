package com.example.demo.dtos;
import java.math.BigDecimal;

import com.example.demo.enums.PropertyType;
import com.example.demo.enums.ReasonType;
import com.example.demo.model.Announcement;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnnouncementListDTO {
    private Long id;
    private String title;
    private String city;
    private String state;
    private String image;
    private PropertyType propertyType;
    private ReasonType reason;
    private Integer rooms;
    private Integer bathRooms;
    private BigDecimal area;
    private BigDecimal price;

    public static AnnouncementListDTO fromEntity(Announcement announcement) {
        return AnnouncementListDTO.builder()
            .id(announcement.getId())
            .title(announcement.getTitle())
            .city(announcement.getCity())
            .state(announcement.getState())
            .image(announcement.getImage())
            .propertyType(announcement.getPropertyType())
            .reason(announcement.getReason())
            .rooms(announcement.getRooms())
            .bathRooms(announcement.getBathRooms())
            .area(announcement.getArea())
            .price(announcement.getPrice())
            .build();
    }
}
