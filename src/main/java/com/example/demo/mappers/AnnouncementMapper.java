package com.example.demo.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.example.demo.dtos.AnnouncementCreateDTO;
import com.example.demo.dtos.AnnouncementDetailDTO;
import com.example.demo.dtos.AnnouncementUpdateDTO;
import com.example.demo.model.Announcement;

@Mapper(componentModel = "spring")
public interface AnnouncementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    Announcement toEntity(AnnouncementCreateDTO dto);

    /** A lista de fotos nao sai da entidade: quem a monta e o servico. */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntity(
        AnnouncementUpdateDTO dto,
        @MappingTarget Announcement entity
    );

    @Mapping(target = "imagens", ignore = true)
    AnnouncementDetailDTO toDetailDTO(Announcement entity);


}
