package com.example.demo.dtos;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size; 

public record UserDTO(

    Long id,

    @NotBlank(message = "Nome completo é obrigatório")
    @Size(max = 50, message = "Nome completo deve ter no máximo 50 caracteres")
    String fullname,

    @Email(message = "Email inválido")
    @Size(max = 40, message = "E-mail deve ter no máximo 40 caracteres")
    String email,

    @NotBlank(message = "Telefone é obrigatório")
    @Size(max = 15, message = "Telefone deve ter no máximo 15 caracteres")
    String phone,

    @NotBlank(message = "Senha é obrigatório")
    @Size(max = 20, message = "Senha deve ter no máximo 20 caracteres")
    String password,

    @NotBlank(message = "Confirmação de senha é obrigatório")
    @Size(max = 20, message = "Confirmação de senha deve ter no máximo 20 caracteres")
    String confirmPassword,

    String photo

) {}
