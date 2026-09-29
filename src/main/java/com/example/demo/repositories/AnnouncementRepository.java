package com.example.demo.repositories;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.enums.PropertyType;
import com.example.demo.enums.ReasonType;
import com.example.demo.model.Announcement;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByUserEmail(String email);

    Page<Announcement> findByUserEmail(String email, Pageable pageable);

    /**
     * Acervo publico com termo e filtros numa consulta so.
     *
     * JPQL de proposito: mantem o ORDER BY do Pageable por nome de propriedade.
     * Em consulta nativa o Spring monta o ORDER BY com o nome cru da coluna, e
     * "createdAt" nao existe no SQL -- a ordenacao da spec 16 cairia junto.
     *
     * Guarda de nulo por filtro em vez de ramificar entre consultas: com cinco
     * filtros opcionais seriam 32 caminhos para manter.
     *
     * `tipos` nunca chega nulo nem vazio. Colecao nula em IN estoura no
     * Hibernate e colecao vazia gera "in ()", que o Postgres recusa; sem filtro
     * de tipo o servico manda o enum inteiro e a condicao vale sempre.
     *
     * Os dois filtros de texto usam string vazia como sentinela de "sem
     * filtro", e nao nulo. Com nulo o Postgres nao consegue inferir o tipo do
     * parametro dentro do LIKE e recusa a consulta inteira com
     * "operator does not exist: text ~~ bytea"; comparar com o literal '' tipa
     * o parametro como texto. Cidade e @NotBlank, entao '' nao colide com
     * cidade real. Nulo segue valendo nos filtros numericos e no enum, onde o
     * Hibernate tipa o parametro pela propria propriedade comparada.
     */
    @Query("""
        select a from Announcement a
        where (:termo = '' or a.searchText like concat('%', :termo, '%'))
          and (:cidade = '' or a.citySearch = :cidade)
          and (:precoMinimo is null or a.price >= :precoMinimo)
          and (:precoMaximo is null or a.price <= :precoMaximo)
          and a.propertyType in :tipos
          and (:finalidade is null or a.reason = :finalidade)
        """)
    Page<Announcement> filtrar(
        @Param("termo") String termo,
        @Param("cidade") String cidade,
        @Param("precoMinimo") BigDecimal precoMinimo,
        @Param("precoMaximo") BigDecimal precoMaximo,
        @Param("tipos") Collection<PropertyType> tipos,
        @Param("finalidade") ReasonType finalidade,
        Pageable pageable
    );

    /** Agregado no banco: carregar a lista inteira so para contar nao escala. */
    long countByUserEmailAndCreatedAtGreaterThanEqual(String email, LocalDateTime inicio);

}
