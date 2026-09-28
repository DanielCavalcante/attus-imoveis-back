package com.example.demo.model;

import java.time.LocalDateTime;

import com.example.demo.enums.AnalysisStatus;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Dossie da pessoa juridica. Nasce com o CNPJ que veio do cadastro e e
 * completado depois; a spec 05 le dossieCompleto para montar a fila de analise.
 *
 * CNPJ e CRECI sao texto: o CNPJ esta migrando para formato alfanumerico, e
 * numero de CRECI nao e grandeza aritmetica.
 */
@Entity
@Table(
    name = "companies",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_company_creci",
        columnNames = {"creci_numero", "creci_uf"}
    )
)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 18, nullable = false)
    private String cnpj;

    @Column(name = "razao_social", length = 120)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 120)
    private String nomeFantasia;

    @Column(name = "creci_numero", length = 20)
    private String creciNumero;

    @Column(name = "creci_uf", length = 2)
    private String creciUf;

    @Column(name = "responsavel_nome", length = 120)
    private String responsavelNome;

    @Column(name = "responsavel_cpf", length = 14)
    private String responsavelCpf;

    @Column(name = "telefone_comercial", length = 15)
    private String telefoneComercial;

    @Column(name = "email_comercial", length = 80)
    private String emailComercial;

    @Column(length = 9)
    private String cep;

    @Column(length = 120)
    private String logradouro;

    @Column(length = 10)
    private String numero;

    @Column(length = 60)
    private String complemento;

    @Column(length = 60)
    private String cidade;

    @Column(length = 2)
    private String uf;

    /**
     * Gravado, nao calculado na leitura: a fila do backoffice filtra por este
     * campo, e recalcular por linha em listagem nao escala.
     */
    @Column(name = "dossie_completo", nullable = false)
    private boolean dossieCompleto = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", length = 20)
    private AnalysisStatus analysisStatus = AnalysisStatus.NAO_ENVIADO;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** E-mail, nao id: a trilha tem de continuar legivel se a conta interna sumir. */
    @Column(name = "reviewed_by", length = 40)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
    }

    public String getCreciNumero() {
        return creciNumero;
    }

    public void setCreciNumero(String creciNumero) {
        this.creciNumero = creciNumero;
    }

    public String getCreciUf() {
        return creciUf;
    }

    public void setCreciUf(String creciUf) {
        this.creciUf = creciUf;
    }

    public String getResponsavelNome() {
        return responsavelNome;
    }

    public void setResponsavelNome(String responsavelNome) {
        this.responsavelNome = responsavelNome;
    }

    public String getResponsavelCpf() {
        return responsavelCpf;
    }

    public void setResponsavelCpf(String responsavelCpf) {
        this.responsavelCpf = responsavelCpf;
    }

    public String getTelefoneComercial() {
        return telefoneComercial;
    }

    public void setTelefoneComercial(String telefoneComercial) {
        this.telefoneComercial = telefoneComercial;
    }

    public String getEmailComercial() {
        return emailComercial;
    }

    public void setEmailComercial(String emailComercial) {
        this.emailComercial = emailComercial;
    }

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

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
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

    public AnalysisStatus getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(AnalysisStatus analysisStatus) {
        this.analysisStatus = analysisStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public boolean isDossieCompleto() {
        return dossieCompleto;
    }

    public void setDossieCompleto(boolean dossieCompleto) {
        this.dossieCompleto = dossieCompleto;
    }
}
