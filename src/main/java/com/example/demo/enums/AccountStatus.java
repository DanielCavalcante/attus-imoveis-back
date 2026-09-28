package com.example.demo.enums;

public enum AccountStatus {

    /**
     * Reservado. Nenhuma conta nasce assim enquanto nao houver servico de
     * e-mail: sem o mecanismo que destranca, o status so trancaria o cadastro.
     */
    PENDENTE_EMAIL("Aguardando confirmacao de e-mail"),

    ATIVO("Ativo");

    private final String description;

    AccountStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
