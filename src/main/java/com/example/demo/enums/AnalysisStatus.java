package com.example.demo.enums;

public enum AnalysisStatus {

    NAO_ENVIADO("Nao enviado para analise"),
    EM_ANALISE("Em analise"),
    APROVADA("Aprovada"),
    REPROVADA("Reprovada");

    private final String description;

    AnalysisStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
