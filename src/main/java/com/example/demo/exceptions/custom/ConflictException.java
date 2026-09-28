package com.example.demo.exceptions.custom;

/** Recurso ja existe. Distingue-se de BusinessException porque responde 409, nao 400. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

}
