package com.example.demo.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Pacote vendido a pessoa juridica.
 *
 * Plano nunca e removido, so desativado: apagar deixaria orfa a assinatura que
 * aponta para ele.
 */
@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 60, nullable = false, unique = true)
    private String nome;

    @Column(length = 300)
    private String descricao;

    /** BigDecimal e nao double: dinheiro nao aceita erro de ponto flutuante. */
    @Column(name = "preco_mensal", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoMensal;

    /**
     * Nulo significa ilimitado. Um numero magico grande viraria limite de
     * verdade no dia em que alguem o ultrapassasse.
     */
    @Column(name = "limite_anuncios")
    private Integer limiteAnuncios;

    @Column(nullable = false)
    private boolean ativo = true;

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPrecoMensal() {
        return precoMensal;
    }

    public void setPrecoMensal(BigDecimal precoMensal) {
        this.precoMensal = precoMensal;
    }

    public Integer getLimiteAnuncios() {
        return limiteAnuncios;
    }

    public void setLimiteAnuncios(Integer limiteAnuncios) {
        this.limiteAnuncios = limiteAnuncios;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
