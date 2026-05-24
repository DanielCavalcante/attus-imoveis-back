package com.example.demo.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.demo.dtos.UserDTO;
import com.example.demo.dtos.UserDetailDTO;
import com.example.demo.dtos.UserListDTO;
import com.example.demo.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Buscar usuários")
    @GetMapping
    public ResponseEntity<List<UserListDTO>> list() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Operation(summary = "Buscar usuário por ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserDetailDTO> findOne(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findOne(id));
    }

    @Operation(summary = "Criar usuário")
    @PostMapping
    public ResponseEntity<UserDetailDTO> create(@Valid @RequestBody UserDTO dto) {
        UserDetailDTO user = userService.create(dto);

        URI uri = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(user.id())
            .toUri();

        return ResponseEntity.created(uri).body(user);
    }

    @Operation(summary = "Atualizar usuário por ID")
    @PutMapping("/{id}")
    public ResponseEntity<UserDetailDTO> update(
        @PathVariable Long id,
        @Valid @RequestBody UserDTO dto
    ) {
        return ResponseEntity.ok(userService.edit(id, dto));
    }

    @Operation(summary = "Remover usuário por ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}