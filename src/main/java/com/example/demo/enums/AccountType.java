package com.example.demo.enums;

/**
 * Identidade da conta, nao capacidade. Navegar, favoritar, contatar anunciante e
 * anunciar valem para PF e PJ; o tipo decide a regra que incide sobre o ato
 * (quota na PF, plano na PJ), nunca se o ato existe.
 *
 * Nao escreva autorizacao contra este enum: quem gateia e o status e o papel.
 */
public enum AccountType {

    PF("Pessoa fisica"),
    PJ("Pessoa juridica"),
    INTERNO("Equipe interna");

    private final String description;

    AccountType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
