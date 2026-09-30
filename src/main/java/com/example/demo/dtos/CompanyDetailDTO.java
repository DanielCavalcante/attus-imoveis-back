package com.example.demo.dtos;

import com.example.demo.enums.AnalysisStatus;

public record CompanyDetailDTO(
    Long id,
    String cnpj,
    String razaoSocial,
    String nomeFantasia,
    String creciNumero,
    String creciUf,
    String responsavelNome,
    String responsavelCpf,
    String telefoneComercial,
    String emailComercial,
    String cep,
    String logradouro,
    String numero,
    String complemento,
    String cidade,
    String uf,
    boolean dossieCompleto,
    AnalysisStatus analysisStatus,
    String rejectionReason
) {}
