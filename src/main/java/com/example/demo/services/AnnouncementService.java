package com.example.demo.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dtos.AnnouncementDTO;
import com.example.demo.model.Announcement;
import com.example.demo.repositories.AnnouncementRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository repository;

    public List<AnnouncementDTO> findAll() {
        return repository.findAll().stream().map(a -> new AnnouncementDTO(a.getId(), a.getFullname(), a.getPhone(), a.getStreet(), a.getStreet(), a.getCep(), a.getCep(), a.getComplement(), a.getPropertyType(), a.getReason())).toList();
    }

    public AnnouncementDTO create(AnnouncementDTO dto) {
        var entity = new Announcement();
        entity.setFullname(dto.fullname());
        entity.setPhone(dto.phone());
        entity.setStreet(dto.street());
        entity.setCep(dto.cep());
        entity.setComplement(dto.complement());
        entity.setPropertyType(dto.propertyType());
        entity.setReason(dto.reason());

        var saved = repository.save(entity);

        return new AnnouncementDTO(
            saved.getId(),
            saved.getFullname(),
            saved.getPhone(),
            saved.getStreet(),
            saved.getStreet(),
            saved.getCep(),
            saved.getCep(),
            saved.getComplement(),
            saved.getPropertyType(),
            saved.getReason()
        );
    }

}
