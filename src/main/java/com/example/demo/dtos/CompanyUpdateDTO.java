package com.example.demo.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada do dossie. Todo campo obrigatorio aqui e um campo que a analise de
 * confiabilidade da spec 05 consulta; complemento e o unico dispensavel.
 */
public record CompanyUpdateDTO(

    @NotBlank(message = "Razao social e obrigatoria")
    @Size(max = 120, message = "Razao social deve ter no maximo 120 caracteres")
    String razaoSocial,

    @NotBlank(message = "Nome fantasia e obrigatorio")
    @Size(max = 120, message = "Nome fantasia deve ter no maximo 120 caracteres")
    String nomeFantasia,

    @NotBlank(message = "Numero do CRECI e obrigatorio")
    @Size(max = 20, message = "CRECI deve ter no maximo 20 caracteres")
    String creciNumero,

    @NotBlank(message = "UF do CRECI e obrigatoria")
    @Size(min = 2, max = 2, message = "UF do CRECI deve ter 2 caracteres")
    String creciUf,

    @NotBlank(message = "Nome do responsavel legal e obrigatorio")
    @Size(max = 120, message = "Nome do responsavel deve ter no maximo 120 caracteres")
    String responsavelNome,

    @NotBlank(message = "CPF do responsavel legal e obrigatorio")
    @Size(max = 14, message = "CPF deve ter no maximo 14 caracteres")
    String responsavelCpf,

    @NotBlank(message = "Telefone comercial e obrigatorio")
    @Size(max = 15, message = "Telefone comercial deve ter no maximo 15 caracteres")
    String telefoneComercial,

    @NotBlank(message = "E-mail comercial e obrigatorio")
    @Email(message = "E-mail comercial invalido")
    @Size(max = 80, message = "E-mail comercial deve ter no maximo 80 caracteres")
    String emailComercial,

    @NotBlank(message = "CEP e obrigatorio")
    @Size(max = 9, message = "CEP deve ter no maximo 9 caracteres")
    String cep,

    @NotBlank(message = "Logradouro e obrigatorio")
    @Size(max = 120, message = "Logradouro deve ter no maximo 120 caracteres")
    String logradouro,

    @NotBlank(message = "Numero e obrigatorio")
    @Size(max = 10, message = "Numero deve ter no maximo 10 caracteres")
    String numero,

    @Size(max = 60, message = "Complemento deve ter no maximo 60 caracteres")
    String complemento,

    @NotBlank(message = "Cidade e obrigatoria")
    @Size(max = 60, message = "Cidade deve ter no maximo 60 caracteres")
    String cidade,

    @NotBlank(message = "UF e obrigatoria")
    @Size(min = 2, max = 2, message = "UF deve ter 2 caracteres")
    String uf

) {}
