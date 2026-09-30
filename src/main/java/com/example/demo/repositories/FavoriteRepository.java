package com.example.demo.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Announcement;
import com.example.demo.model.Favorite;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserEmailAndAnnouncementId(String email, Long announcementId);

    /**
     * O que esta fora do ar some da lista, mas o favorito continua gravado.
     *
     * O favorito e da pessoa, nao do anunciante: apaga-lo porque o dono tirou o
     * anuncio do ar destruiria dado de terceiro. Reativado, ele volta sozinho.
     *
     * O countQuery filtra junto. Filtrar so a consulta principal daria pagina
     * com menos itens do que o total anunciado, e quem navega descobriria
     * paginas vazias no fim.
     */
    @Query(
        value = """
            select f.announcement from Favorite f
            where f.user.email = :email and f.announcement.active = true
            order by f.createdAt desc
            """,
        countQuery = """
            select count(f) from Favorite f
            where f.user.email = :email and f.announcement.active = true
            """
    )
    Page<Announcement> findAnnouncementsByUserEmail(String email, Pageable pageable);
}
