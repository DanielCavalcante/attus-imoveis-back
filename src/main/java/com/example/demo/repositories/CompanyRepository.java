package com.example.demo.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.example.demo.enums.AnalysisStatus;
import com.example.demo.model.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByUserId(Long userId);

    Optional<Company> findByUserEmail(String email);

    boolean existsByCreciNumeroAndCreciUf(String creciNumero, String creciUf);

    List<Company> findAllByAnalysisStatus(AnalysisStatus analysisStatus);
}
