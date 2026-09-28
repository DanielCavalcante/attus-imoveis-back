package com.example.demo.dtos;

import java.math.BigDecimal;

import com.example.demo.model.Plan;

public record PlanDTO(
    Long id,
    String nome,
    String descricao,
    BigDecimal precoMensal,
    Integer limiteAnuncios,
    boolean anunciosIlimitados,
    boolean ativo
) {

    public static PlanDTO fromEntity(Plan plan) {
        return new PlanDTO(
            plan.getId(),
            plan.getNome(),
            plan.getDescricao(),
            plan.getPrecoMensal(),
            plan.getLimiteAnuncios(),
            plan.getLimiteAnuncios() == null,
            plan.isAtivo()
        );
    }
}
