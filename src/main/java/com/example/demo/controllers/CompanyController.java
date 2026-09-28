package com.example.demo.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dtos.CompanyDetailDTO;
import com.example.demo.dtos.CompanyUpdateDTO;
import com.example.demo.services.CompanyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Companies")
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "Dossie da empresa da conta autenticada")
    @GetMapping("/me")
    public ResponseEntity<CompanyDetailDTO> findMine(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(companyService.findByAccount(userDetails.getUsername()));
    }

    @Operation(summary = "Completa o dossie da empresa da conta autenticada")
    @PutMapping("/me")
    public ResponseEntity<CompanyDetailDTO> updateMine(
        @Valid @RequestBody CompanyUpdateDTO dto,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(companyService.update(userDetails.getUsername(), dto));
    }

    @Operation(summary = "Envia o dossie da empresa para analise da equipe interna")
    @PostMapping("/me/analise")
    public ResponseEntity<CompanyDetailDTO> submitForAnalysis(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(companyService.submitForAnalysis(userDetails.getUsername()));
    }
}
