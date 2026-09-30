package com.example.demo.dtos;

import java.time.LocalDateTime;

import com.example.demo.model.Subscription;

/** O plano vigente de uma empresa, e a trilha de quem o liberou. */
public record SubscriptionDTO(
    Long id,
    Long companyId,
    Long planId,
    String planoNome,
    Integer limiteAnuncios,
    boolean anunciosIlimitados,
    String liberadoPor,
    LocalDateTime liberadoEm
) {

    public static SubscriptionDTO fromEntity(Subscription s) {
        return new SubscriptionDTO(
            s.getId(),
            s.getCompany().getId(),
            s.getPlan().getId(),
            s.getPlan().getNome(),
            s.getPlan().getLimiteAnuncios(),
            s.getPlan().getLimiteAnuncios() == null,
            s.getLiberadoPor(),
            s.getLiberadoEm()
        );
    }
}
