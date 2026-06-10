package com.example.demo.services;

import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.example.demo.dtos.AnnouncementCreateDTO;
import com.example.demo.dtos.AnnouncementDetailDTO;
import com.example.demo.dtos.AnnouncementListDTO;
import com.example.demo.dtos.AnnouncementUpdateDTO;
import com.example.demo.exceptions.custom.ResourceNotFoundException;
import com.example.demo.mappers.AnnouncementMapper;
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
    private final AnnouncementMapper mapper;

    @Transactional(readOnly = true)
    public List<AnnouncementListDTO> findAll() {
        return repository.findAll().stream().map(AnnouncementListDTO::fromEntity).toList();
    }

    @Transactional
    public AnnouncementDetailDTO create(AnnouncementCreateDTO dto, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        Announcement entity = mapper.toEntity(dto);
        entity.setUser(user);

        Announcement saved = repository.save(entity);

        return mapper.toDetailDTO(saved);
    }

    @SuppressWarnings("null")
    @Transactional
    public AnnouncementDetailDTO edit(@NonNull Long id, AnnouncementUpdateDTO dto, String email) {
        Announcement entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Anúncio não encontrado"));
        mapper.updateEntity(dto, entity);
        Announcement saved = repository.save(entity);

        return mapper.toDetailDTO(saved);
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

    @Transactional(readOnly = true)
    public AnnouncementDetailDTO findOne(@NonNull Long id) {
        Announcement announcement = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Anúncio não encontrado com id: " + id));
        return toDetailDTO(announcement);
    }

}
