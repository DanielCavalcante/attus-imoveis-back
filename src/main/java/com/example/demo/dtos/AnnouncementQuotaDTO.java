package com.example.demo.dtos;

/**
 * Saldo de anuncios no ar, e se a conta pode publicar agora.
 *
 * Conta sem teto tem saldo, nao tem limite: devolve quantos mantem no ar e
 * deixa `limite` e `restantes` nulos, em vez de um numero inventado. Contar e
 * de todo mundo; limitar e que varia por tipo de conta.
 *
 * `podePublicar` existe porque saldo nao e a unica razao de uma conta nao
 * publicar -- a empresa pode estar sem aprovacao, e uma tela que olhasse so os
 * numeros ofereceria o formulario para quem o servidor vai recusar. O `motivo`
 * e codigo estavel, nao frase: quem consome escolhe o texto, e texto reescrito
 * nunca quebra a tela.
 */
public record AnnouncementQuotaDTO(
    boolean ilimitado,
    Integer limite,
    Long usados,
    Long restantes,
    boolean podePublicar,
    String motivo
) {

    /** Sem vaga: inativar ou excluir abre uma. */
    public static final String QUOTA_CHEIA = "QUOTA_CHEIA";

    public static AnnouncementQuotaDTO semLimite(long usados) {
        return new AnnouncementQuotaDTO(true, null, usados, null, true, null);
    }

    public static AnnouncementQuotaDTO de(int limite, long usados) {
        long restantes = Math.max(0, limite - usados);

        return new AnnouncementQuotaDTO(
            false,
            limite,
            usados,
            restantes,
            restantes > 0,
            restantes > 0 ? null : QUOTA_CHEIA
        );
    }

    /**
     * O mesmo saldo, com a publicacao trancada por razao que nao e a vaga.
     *
     * O saldo continua verdadeiro e continua sendo mostrado: a empresa sem
     * aprovacao tem direito de ver quantos anuncios mantem no ar.
     */
    public AnnouncementQuotaDTO bloqueadaPor(String motivo) {
        return new AnnouncementQuotaDTO(ilimitado, limite, usados, restantes, false, motivo);
    }
}
