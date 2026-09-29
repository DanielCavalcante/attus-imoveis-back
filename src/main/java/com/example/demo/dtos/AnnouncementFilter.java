package com.example.demo.dtos;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import com.example.demo.enums.PropertyType;
import com.example.demo.enums.ReasonType;
import com.example.demo.model.Announcement;

/**
 * Os recortes do acervo publico, ja normalizados para a consulta.
 *
 * Existe como tipo e nao como seis parametros soltos porque o servico e o
 * controlador passariam a mesma sequencia de argumentos em duas assinaturas, e
 * trocar dois BigDecimal de lugar compila em silencio -- foi assim que preco e
 * area ja sairam trocados neste repositorio.
 */
public record AnnouncementFilter(
    String termo,
    String cidade,
    String bairro,
    BigDecimal precoMinimo,
    BigDecimal precoMaximo,
    Collection<PropertyType> tipos,
    ReasonType finalidade
) {

    /**
     * Valor ausente, vazio ou so espaco significa "sem filtro", nao "nada
     * encontrado": e a mesma regra que a spec 16 fixou para o termo.
     *
     * Texto ausente vira string vazia, nao nulo: com nulo o Postgres nao
     * infere o tipo do parametro dentro do LIKE e recusa a consulta. Tipo
     * ausente vira o enum inteiro, pelo mesmo motivo do lado das colecoes --
     * colecao nula em IN estoura no Hibernate.
     */
    public static AnnouncementFilter de(
        String termo,
        String cidade,
        String bairro,
        BigDecimal precoMinimo,
        BigDecimal precoMaximo,
        List<PropertyType> tipos,
        ReasonType finalidade
    ) {
        return new AnnouncementFilter(
            normalizado(termo),
            normalizado(cidade),
            normalizado(bairro),
            precoMinimo,
            precoMaximo,
            tipos == null || tipos.isEmpty() ? EnumSet.allOf(PropertyType.class) : tipos,
            finalidade
        );
    }

    private static String normalizado(String valor) {
        return valor == null || valor.isBlank()
            ? ""
            : Announcement.normalizar(valor.trim());
    }
}
