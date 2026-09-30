package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Um CEP que ja foi resolvido, guardado para nao perguntar de novo. */
@Entity
@Table(name = "enderecos")
public class Endereco {

    /** So digitos: e a forma normalizada com que se procura. */
    @Id
    @Column(length = 8)
    private String cep;

    @Column(length = 120)
    private String logradouro;

    /** Nulo de verdade: cidade com CEP unico nao tem bairro. */
    @Column(length = 60)
    private String bairro;

    @Column(length = 60, nullable = false)
    private String cidade;

    @Column(length = 2, nullable = false)
    private String uf;

    @Column(length = 40, nullable = false)
    private String provedor;

    @Column(name = "resolvido_em", nullable = false)
    private LocalDateTime resolvidoEm = LocalDateTime.now();

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getProvedor() {
        return provedor;
    }

    public void setProvedor(String provedor) {
        this.provedor = provedor;
    }

    public LocalDateTime getResolvidoEm() {
        return resolvidoEm;
    }

    public void setResolvidoEm(LocalDateTime resolvidoEm) {
        this.resolvidoEm = resolvidoEm;
    }
}
