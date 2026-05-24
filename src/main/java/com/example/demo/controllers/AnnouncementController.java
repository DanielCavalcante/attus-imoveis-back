package com.example.demo.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.demo.dtos.AnnouncementCreateDTO;
import com.example.demo.dtos.AnnouncementDetailDTO;
import com.example.demo.dtos.AnnouncementListDTO;
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

    // @GetMapping("/{id}")
    // public AnnouncementCreateDTO findOne(@PathVariable Long id) {
    //     return service.findOne(id);
    // }

    @Operation(summary = "Criar anúncio")
    @PostMapping
    public ResponseEntity<AnnouncementDetailDTO> create(
        @Valid @RequestBody AnnouncementCreateDTO dto, 
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        System.out.println(userDetails.getUsername());
        return ResponseEntity.ok(announcementService.create(dto, userDetails.getUsername()));
    }

    // @PutMapping("/{id}")
    // public AnnouncementDTO update(
    //     @PathVariable Long id,
    //     @RequestBody AnnouncementDTO dto
    // ) {
    //     return service.edit(id, dto);
    // }

    // 🗑️ DELETAR
    // @DeleteMapping("/{id}")
    // public void delete(@PathVariable Long id) {
    //     service.delete(id);
    // }
}