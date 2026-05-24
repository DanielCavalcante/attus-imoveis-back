package com.example.demo.enums;

public enum ReasonType {

    SALE("Venda"),
    RENT("Alugar");

    private final String description;

    ReasonType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}