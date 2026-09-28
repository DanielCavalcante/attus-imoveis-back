package com.example.demo.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Announcement;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByUserEmail(String email);

    /** Agregado no banco: carregar a lista inteira so para contar nao escala. */
    long countByUserEmailAndCreatedAtGreaterThanEqual(String email, LocalDateTime inicio);

}
