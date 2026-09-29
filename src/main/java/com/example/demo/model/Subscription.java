package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * O plano de uma empresa, liberado a mao enquanto nao ha gateway.
 *
 * Uma linha por liberacao, nao uma por empresa: trocar de plano fecha a
 * anterior e abre outra, entao a trilha de quem liberou o que e quando fica
 * inteira. So uma linha por empresa fica `vigente`, e quem garante isso e o
 * indice unico parcial da V9.
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false)
    private boolean vigente = true;

    /** E-mail, nao id: a trilha continua legivel se a conta interna sumir. */
    @Column(name = "liberado_por", length = 80, nullable = false)
    private String liberadoPor;

    @Column(name = "liberado_em", nullable = false)
    private LocalDateTime liberadoEm = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public boolean isVigente() {
        return vigente;
    }

    public void setVigente(boolean vigente) {
        this.vigente = vigente;
    }

    public String getLiberadoPor() {
        return liberadoPor;
    }

    public void setLiberadoPor(String liberadoPor) {
        this.liberadoPor = liberadoPor;
    }

    public LocalDateTime getLiberadoEm() {
        return liberadoEm;
    }

    public void setLiberadoEm(LocalDateTime liberadoEm) {
        this.liberadoEm = liberadoEm;
    }
}
