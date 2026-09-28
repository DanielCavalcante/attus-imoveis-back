package com.example.demo.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Announcement;
import com.example.demo.model.AnnouncementView;

@Repository
public interface AnnouncementViewRepository extends JpaRepository<AnnouncementView, Long> {

    Optional<AnnouncementView> findByUserEmailAndAnnouncementId(String email, Long announcementId);

    @Query("""
        select v.announcement from AnnouncementView v
        where v.user.email = :email
        order by v.viewedAt desc
        """)
    List<Announcement> findAnnouncementsByUserEmail(String email);
}
