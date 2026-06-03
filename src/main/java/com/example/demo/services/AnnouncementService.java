package com.example.demo.services;

import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.example.demo.dtos.AnnouncementCreateDTO;
import com.example.demo.dtos.AnnouncementDetailDTO;
import com.example.demo.dtos.AnnouncementListDTO;
import com.example.demo.exceptions.custom.ResourceNotFoundException;
import com.example.demo.model.Announcement;
import com.example.demo.model.User;
import com.example.demo.repositories.AnnouncementRepository;
import com.example.demo.repositories.UserRepository;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository repository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AnnouncementListDTO> findAll() {
        return repository.findAll().stream().map(AnnouncementListDTO::fromEntity).toList();
    }

    @Transactional
    public AnnouncementDetailDTO create(AnnouncementCreateDTO dto, String email) {
        Announcement entity = new Announcement();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        mapDtoToEntity(dto, entity, user);

        Announcement saved = repository.save(entity);

        return toDetailDTO(saved);
    }

    private void mapDtoToEntity(AnnouncementCreateDTO dto, Announcement entity, User user) {
        entity.setTitle(dto.title());
        entity.setDescription(dto.description());
        entity.setCity(dto.city());
        entity.setState(dto.state());
        entity.setImage(dto.image());
        entity.setStreet(dto.street());
        entity.setStreetNumber(dto.streetNumber());
        entity.setCep(dto.cep());
        entity.setComplement(dto.complement());
        entity.setRooms(dto.rooms());
        entity.setBathRooms(dto.bathRooms());
        entity.setArea(dto.area());
        entity.setPrice(dto.price());
        entity.setPropertyType(dto.propertyType());
        entity.setReason(dto.reason());
        entity.setUser(user);
    }

    private AnnouncementDetailDTO toDetailDTO(Announcement announcement) {
        return new AnnouncementDetailDTO(
            announcement.getId(), 
            announcement.getTitle(),
            announcement.getDescription(),
            announcement.getCity(),
            announcement.getState(),
            announcement.getStreet(),
            announcement.getImage(),
            announcement.getStreetNumber(),
            announcement.getCep(),
            announcement.getComplement(),
            announcement.getPropertyType(),
            announcement.getReason(),
            announcement.getRooms(),
            announcement.getBathRooms(),
            announcement.getPrice(),
            announcement.getArea()
        );
    }

    @Transactional(readOnly = true)
    public List<AnnouncementListDTO> findByUser(String email) {
        List<Announcement> announcements = repository.findByUserEmail(email);
        return announcements.stream().map(AnnouncementListDTO::fromEntity).toList();
    }

    public AnnouncementDetailDTO findOne(@NonNull Long id) {
        Announcement announcement = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Anúncio não encontrado com id: " + id));

        return toDetailDTO(announcement);
    }

}
