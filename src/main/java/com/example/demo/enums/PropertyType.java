package com.example.demo.enums;

public enum PropertyType {

    HOUSE("Casa"),
    APARTMENT("Apartamento"),
    CONDOMINIUM("Condomínio"),
    LAND("Terreno");

    private final String description;

    PropertyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}