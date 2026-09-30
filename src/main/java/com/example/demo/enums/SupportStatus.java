package com.example.demo.enums;

public enum SupportStatus {

    ABERTO("Aguardando a equipe"),
    CONCLUIDO("Atendido");

    private final String description;

    SupportStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
