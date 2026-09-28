package com.example.demo.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PlanUpdateDTO(

    @NotBlank(message = "Nome do plano e obrigatorio")
    @Size(max = 60, message = "Nome deve ter no maximo 60 caracteres")
    String nome,

    @Size(max = 300, message = "Descricao deve ter no maximo 300 caracteres")
    String descricao,

    @NotNull(message = "Preco mensal e obrigatorio")
    @PositiveOrZero(message = "Preco mensal nao pode ser negativo")
    BigDecimal precoMensal,

    @PositiveOrZero(message = "Limite de anuncios nao pode ser negativo")
    Integer limiteAnuncios

) {}
