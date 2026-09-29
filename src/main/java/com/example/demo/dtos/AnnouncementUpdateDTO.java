package com.example.demo.dtos;
import java.math.BigDecimal;
import java.util.List;

import com.example.demo.enums.PropertyType;
import com.example.demo.enums.ReasonType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size; 

public record AnnouncementUpdateDTO(

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 120, message = "Título deve ter no máximo 120 caracteres")
    String title,

    String description,

    @NotBlank(message = "Cidade é obrigatória")
    @Size(max = 40)
    String city,

    @NotBlank(message = "Estado é obrigatória")
    @Size(max = 20)
    String state,

    @NotBlank(message = "Rua é obrigatória")
    @Size(max = 80)
    String street,

    /**
     * A capa, espelhada. Continua aqui porque a listagem e o anuncio antigo
     * dependem dela; quando `imagens` vem preenchida, quem manda e ela e o
     * servico sobrescreve este campo.
     */
    @Size(max = 244, message = "A referência da imagem deve ter no máximo 244 caracteres")
    // E isto que faz a mudanca ser seguranca, e nao interface: sem recusar o
    // endereco externo aqui, bastaria nao usar a tela para voltar a apontar
    // para qualquer host. A imagem tem de ter sido enviada para o bucket.
    @Pattern(
        regexp = "^(?!\s*https?://).*",
        message = "A imagem deve ser enviada, e não um endereço externo"
    )
    String image,

    /** As fotos do anuncio, na ordem em que aparecem. Vazia mantem so `image`. */
    List<@Pattern(
        regexp = "^(?!\s*https?://).*",
        message = "A imagem deve ser enviada, e não um endereço externo"
    ) String> imagens,

    /** Qual das `imagens` e a capa. Nulo significa a primeira. */
    @PositiveOrZero(message = "Índice de capa inválido")
    Integer capa,

    @NotNull(message = "Número é obrigatório")
    @Positive(message = "Número inválido")
    Integer streetNumber,

    @NotBlank(message = "CEP é obrigatório")
    @Pattern(
        regexp = "^\\d{5}-?\\d{3}$",
        message = "CEP inválido"
    )
    String cep,

    /** Sai do CEP quando o anunciante nao informa; informado, prevalece. */
    @Size(max = 60)
    String neighborhood,

    @Size(max = 60)
    String complement,

    @NotNull(message = "Tipo do imóvel é obrigatório")
    PropertyType propertyType,

    @NotNull(message = "Razão é obrigatória")
    ReasonType reason,

    @NotNull(message = "Quantidade de quartos é obrigatória")
    @PositiveOrZero(message = "Quartos inválidos")
    Integer rooms,

    @NotNull(message = "Quantidade de banheiros é obrigatória")
    @PositiveOrZero(message = "Banheiros inválidos")
    Integer bathRooms,

    @NotNull(message = "Área é obrigatória")
    @Positive(message = "Área inválida")
    BigDecimal area,

    @NotNull(message = "Preço é obrigatório")
    @Positive(message = "Preço deve ser maior que zero")
    BigDecimal price
) {}
