package com.example.demo.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.enums.AnalysisStatus;
import com.example.demo.model.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByUserId(Long userId);

    Optional<Company> findByUserEmail(String email);

    boolean existsByCreciNumeroAndCreciUf(String creciNumero, String creciUf);

    /** Ordem de atendimento: quem chegou primeiro. Company nao tem instante de criacao. */
    Page<Company> findAllByAnalysisStatus(AnalysisStatus analysisStatus, Pageable pageable);
}
