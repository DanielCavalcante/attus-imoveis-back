package com.example.demo.dtos;

/**
 * Saldo de publicacao do mes corrente. Conta sem limite (PJ) devolve
 * ilimitado = true e os demais campos nulos, em vez de um numero inventado.
 */
public record AnnouncementQuotaDTO(
    boolean ilimitado,
    Integer limite,
    Long usados,
    Long restantes
) {

    public static AnnouncementQuotaDTO semLimite() {
        return new AnnouncementQuotaDTO(true, null, null, null);
    }

    public static AnnouncementQuotaDTO de(int limite, long usados) {
        return new AnnouncementQuotaDTO(false, limite, usados, Math.max(0, limite - usados));
    }
}
