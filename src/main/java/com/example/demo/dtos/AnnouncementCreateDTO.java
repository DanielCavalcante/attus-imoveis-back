package com.example.demo.dtos;
import java.math.BigDecimal;

import com.example.demo.enums.PropertyType;
import com.example.demo.enums.ReasonType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size; 

public record AnnouncementCreateDTO(

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

    @NotBlank(message = "O link da imagem do anúncio é obrigatório")
    @Size(max = 100, message = "O link da imagem deve ter no máximo 100 carateres")
    String image,

    @NotNull(message = "Número é obrigatório")
    @Positive(message = "Número inválido")
    Integer streetNumber,

    @NotBlank(message = "CEP é obrigatório")
    @Pattern(
        regexp = "^\\d{5}-?\\d{3}$",
        message = "CEP inválido"
    )
    String cep,

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
