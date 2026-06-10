package com.example.demo.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.demo.dtos.AnnouncementCreateDTO;
import com.example.demo.dtos.AnnouncementDetailDTO;
import com.example.demo.dtos.AnnouncementListDTO;
import com.example.demo.dtos.AnnouncementUpdateDTO;
import com.example.demo.services.AnnouncementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Announcements")
@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "Buscar anúncios")
    @GetMapping
    public ResponseEntity<List<AnnouncementListDTO>> list() {
        return ResponseEntity.ok(announcementService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementDetailDTO> findOne(@PathVariable @NonNull Long id) {
        return ResponseEntity.ok(announcementService.findOne(id));
    }

    @Operation(summary = "Criar anúncio")
    @PostMapping
    public ResponseEntity<AnnouncementDetailDTO> create(
        @Valid @RequestBody AnnouncementCreateDTO dto, 
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(announcementService.create(dto, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementDetailDTO> update(
        @PathVariable Long id,
        @Valid @RequestBody AnnouncementUpdateDTO dto,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(announcementService.edit(id, dto, userDetails.getUsername()));
    }

    // @DeleteMapping("/{id}")
    // public void delete(@PathVariable Long id) {
    //     service.delete(id);
    // }

    @GetMapping("/me")
    public ResponseEntity<List<AnnouncementListDTO>> myAnnouncements(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(announcementService.findByUser(userDetails.getUsername()));
    }
}