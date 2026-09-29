package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Um bairro do catalogo de uma cidade.
 *
 * Existe para a cidade cujo catalogo foi semeado; cidade sem catalogo continua
 * tendo seus bairros derivados do distinct sobre os anuncios, como a spec 19
 * decidiu. O catalogo alimenta as OPCOES de filtro, nunca o dado do anuncio --
 * o bairro do anuncio segue vindo do CEP.
 */
@Entity
@Table(name = "neighborhoods")
public class Neighborhood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cidade ja normalizada: e por ela que o catalogo e procurado. */
    @NotBlank
    @Size(max = 40)
    @Column(name = "city_search", length = 40, nullable = false)
    private String citySearch;

    @NotBlank
    @Size(max = 60)
    @Column(length = 60, nullable = false)
    private String name;

    @Column(name = "name_search", length = 60, nullable = false)
    private String nameSearch;

    /** Mesma normalizacao de search_text, city_search e neighborhood_search. */
    @PrePersist
    @PreUpdate
    private void normalizar() {
        citySearch = Announcement.normalizar(citySearch == null ? "" : citySearch);
        nameSearch = Announcement.normalizar(name == null ? "" : name);
    }

    public Long getId() {
        return id;
    }

    public String getCitySearch() {
        return citySearch;
    }

    public void setCitySearch(String citySearch) {
        this.citySearch = citySearch;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameSearch() {
        return nameSearch;
    }
}
