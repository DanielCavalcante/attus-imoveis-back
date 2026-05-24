package com.example.demo.exceptions.custom;

public class ResourceNotFoundException
    extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}