package com.example.demo.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.AnnouncementImage;

@Repository
public interface AnnouncementImageRepository extends JpaRepository<AnnouncementImage, Long> {

    /** Na ordem em que foram enviadas: e ela que a galeria apresenta. */
    List<AnnouncementImage> findByAnnouncementIdOrderByPosicaoAsc(Long announcementId);

    void deleteByAnnouncementId(Long announcementId);
}
