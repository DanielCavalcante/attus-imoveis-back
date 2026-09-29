package com.example.demo.dtos;

/**
 * Saldo de anuncios no ar.
 *
 * Conta sem teto tem saldo, nao tem limite: devolve quantos mantem no ar e
 * deixa `limite` e `restantes` nulos, em vez de um numero inventado. Contar e
 * de todo mundo; limitar e que varia por tipo de conta.
 */
public record AnnouncementQuotaDTO(
    boolean ilimitado,
    Integer limite,
    Long usados,
    Long restantes
) {

    public static AnnouncementQuotaDTO semLimite(long usados) {
        return new AnnouncementQuotaDTO(true, null, usados, null);
    }

    public static AnnouncementQuotaDTO de(int limite, long usados) {
        return new AnnouncementQuotaDTO(false, limite, usados, Math.max(0, limite - usados));
    }
}
