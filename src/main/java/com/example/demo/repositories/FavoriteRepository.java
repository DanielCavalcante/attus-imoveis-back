package com.example.demo.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Announcement;
import com.example.demo.model.Favorite;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserEmailAndAnnouncementId(String email, Long announcementId);

    @Query("""
        select f.announcement from Favorite f
        where f.user.email = :email
        order by f.createdAt desc
        """)
    List<Announcement> findAnnouncementsByUserEmail(String email);
}
