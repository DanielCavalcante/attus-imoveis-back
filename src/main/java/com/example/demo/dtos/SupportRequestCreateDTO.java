package com.example.demo.dtos;

import jakarta.validation.constraints.Size;

public record SupportRequestCreateDTO(

    @Size(max = 500, message = "Mensagem deve ter no maximo 500 caracteres")
    String mensagem

) {}
