package com.example.demo.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.enums.SupportStatus;
import com.example.demo.model.SupportRequest;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {

    boolean existsByAnnouncementIdAndStatus(Long announcementId, SupportStatus status);

    Optional<SupportRequest> findByAnnouncementIdAndStatus(Long announcementId, SupportStatus status);

    List<SupportRequest> findAllByStatus(SupportStatus status);
}
