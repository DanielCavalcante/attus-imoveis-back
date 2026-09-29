package com.example.demo.dtos;

/**
 * Linha da tela de liberacao de plano.
 *
 * Traz junto quantos anuncios a empresa mantem no ar: e a metade do aviso de
 * downgrade, e busca-la por linha, depois, seria uma consulta por empresa.
 */
public record CompanyPlanListDTO(
    Long companyId,
    String razaoSocial,
    String nomeFantasia,
    String cnpj,
    String planoNome,
    Integer limiteAnuncios,
    long anunciosNoAr
) {}
