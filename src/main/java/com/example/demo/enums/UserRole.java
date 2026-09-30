package com.example.demo.enums;

public enum UserRole {

    COMUM,
    INTERNO;

    /** Prefixo exigido pelo hasRole do Spring Security. */
    public String authority() {
        return "ROLE_" + name();
    }
}
