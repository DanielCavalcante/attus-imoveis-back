package com.example.demo.model;

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
 * Uma foto do anuncio.
 *
 * Guarda o nome do objeto no bucket, nao o caminho da rota: quem monta o
 * caminho e o servico, e gravar o caminho pronto prenderia o banco ao formato
 * da URL de hoje.
 */
@Entity
@Table(name = "announcement_images")
public class AnnouncementImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Column(name = "object_name", length = 120, nullable = false)
    private String objectName;

    @Column(name = "content_type", length = 40, nullable = false)
    private String contentType;

    @Column(nullable = false)
    private int posicao;

    @Column(nullable = false)
    private boolean capa;

    public Long getId() {
        return id;
    }

    public Announcement getAnnouncement() {
        return announcement;
    }

    public void setAnnouncement(Announcement announcement) {
        this.announcement = announcement;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getPosicao() {
        return posicao;
    }

    public void setPosicao(int posicao) {
        this.posicao = posicao;
    }

    public boolean isCapa() {
        return capa;
    }

    public void setCapa(boolean capa) {
        this.capa = capa;
    }
}
