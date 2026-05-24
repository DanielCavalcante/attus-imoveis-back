package com.example.demo.exceptions.models;

import java.time.LocalDateTime;

public record ApiError(
    Integer status,
    String message,
    LocalDateTime timestamp
) {}