package com.example.demo.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.demo.dtos.UserDTO;
import com.example.demo.dtos.UserDetailDTO;
import com.example.demo.dtos.UserListDTO;
import com.example.demo.dtos.UserUpdateDTO;
import com.example.demo.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** So a equipe interna: o SecurityConfig fecha esta rota por papel. */
    @Operation(summary = "Buscar usuários")
    @GetMapping
    public ResponseEntity<List<UserListDTO>> list() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Operation(summary = "Buscar usuário por ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserDetailDTO> findOne(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(userService.findOne(id, userDetails.getUsername()));
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
        @Valid @RequestBody UserUpdateDTO dto,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(userService.edit(id, dto, userDetails.getUsername()));
    }

    @Operation(summary = "Remover usuário por ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        userService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}