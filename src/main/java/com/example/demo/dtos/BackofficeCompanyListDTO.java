package com.example.demo.dtos;

import java.time.LocalDateTime;

import com.example.demo.enums.AnalysisStatus;

/** Linha da fila de analise. Traz o que decide, nao o dossie inteiro. */
public record BackofficeCompanyListDTO(
    Long id,
    String cnpj,
    String razaoSocial,
    String nomeFantasia,
    String creciNumero,
    String creciUf,
    String responsavelNome,
    String emailComercial,
    AnalysisStatus analysisStatus,
    LocalDateTime reviewedAt
) {}
