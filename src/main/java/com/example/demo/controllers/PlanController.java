package com.example.demo.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dtos.PlanDTO;
import com.example.demo.services.PlanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Catalogo publico. Preco de pacote e informacao de venda, nao dado protegido. */
@Tag(name = "Plans")
@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @Operation(summary = "Planos disponiveis para contratacao")
    @GetMapping
    public ResponseEntity<List<PlanDTO>> active() {
        return ResponseEntity.ok(planService.findActive());
    }
}
