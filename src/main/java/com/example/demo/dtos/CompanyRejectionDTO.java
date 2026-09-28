package com.example.demo.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRejectionDTO(

    @NotBlank(message = "Motivo da reprovacao e obrigatorio")
    @Size(max = 500, message = "Motivo deve ter no maximo 500 caracteres")
    String motivo

) {}
