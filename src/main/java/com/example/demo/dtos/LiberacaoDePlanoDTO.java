package com.example.demo.dtos;

import jakarta.validation.constraints.NotNull;

/** Qual plano liberar. Quem libera vem da sessao, nao do corpo. */
public record LiberacaoDePlanoDTO(
    @NotNull(message = "Plano é obrigatório")
    Long planId
) {}
