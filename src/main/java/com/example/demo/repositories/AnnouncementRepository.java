package com.example.demo.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Announcement;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByUserEmail(String email);

    Page<Announcement> findByUserEmail(String email, Pageable pageable);

    /** JPQL de proposito: mantem o ORDER BY do Pageable por nome de propriedade. */
    @Query("select a from Announcement a where a.searchText like concat('%', :termo, '%')")
    Page<Announcement> buscar(@Param("termo") String termo, Pageable pageable);

    /** Agregado no banco: carregar a lista inteira so para contar nao escala. */
    long countByUserEmailAndCreatedAtGreaterThanEqual(String email, LocalDateTime inicio);

}
