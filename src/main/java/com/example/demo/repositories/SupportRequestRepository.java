package com.example.demo.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.enums.SupportStatus;
import com.example.demo.model.SupportRequest;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {

    boolean existsByAnnouncementIdAndStatus(Long announcementId, SupportStatus status);

    Optional<SupportRequest> findByAnnouncementIdAndStatus(Long announcementId, SupportStatus status);

    /**
     * join fetch explicito, e nao @EntityGraph: com EntityGraph a consulta
     * paginada media 6 acessos para 5 pedidos, ou seja, seguia carregando as
     * associacoes uma a uma. Montar a linha da fila desreferencia anuncio e
     * solicitante, entao os dois vem na mesma consulta.
     *
     * Associacao para-um nao multiplica linha, entao a paginacao continua
     * acontecendo no SQL; a contagem tem consulta propria.
     */
    @Query(
        value = """
            select r from SupportRequest r
            join fetch r.announcement
            join fetch r.user
            where r.status = :status
            """,
        countQuery = "select count(r) from SupportRequest r where r.status = :status"
    )
    Page<SupportRequest> findAllByStatus(@Param("status") SupportStatus status, Pageable pageable);
}
