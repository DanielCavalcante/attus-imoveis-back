package com.example.demo.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.example.demo.dtos.AnnouncementDTO;
import com.example.demo.services.AnnouncementService;

@RestController
@RequestMapping("/announcements")
public class AnnouncementController {

    private final AnnouncementService service;

    public AnnouncementController(AnnouncementService service) {
        this.service = service;
    }

    // 🔍 LISTAR TODOS
    @GetMapping
    public List<AnnouncementDTO> list() {
        return service.findAll();
    }

    // @GetMapping("/{id}")
    // public AnnouncementDTO findOne(@PathVariable Long id) {
    //     return service.findOne(id);
    // }

    // ➕ CRIAR
    @PostMapping
    public AnnouncementDTO create(@RequestBody AnnouncementDTO dto) {
        return service.create(dto);
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