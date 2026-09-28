package com.example.demo.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Announcement;
import com.example.demo.model.AnnouncementView;

@Repository
public interface AnnouncementViewRepository extends JpaRepository<AnnouncementView, Long> {

    Optional<AnnouncementView> findByUserEmailAndAnnouncementId(String email, Long announcementId);

    @Query(
        value = """
            select v.announcement from AnnouncementView v
            where v.user.email = :email
            order by v.viewedAt desc
            """,
        countQuery = "select count(v) from AnnouncementView v where v.user.email = :email"
    )
    Page<Announcement> findAnnouncementsByUserEmail(String email, Pageable pageable);
}
