package com.example.demo.exceptions.custom;

public class BusinessException extends RuntimeException {
    
    public BusinessException(String message) {
        super(message);
    }

}
