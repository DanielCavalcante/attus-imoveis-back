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

    @Query(
        value = """
            select f.announcement from Favorite f
            where f.user.email = :email
            order by f.createdAt desc
            """,
        countQuery = "select count(f) from Favorite f where f.user.email = :email"
    )
    Page<Announcement> findAnnouncementsByUserEmail(String email, Pageable pageable);
}
