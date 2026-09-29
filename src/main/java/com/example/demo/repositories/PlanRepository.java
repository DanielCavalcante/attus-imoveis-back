package com.example.demo.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Plan;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    boolean existsByNome(String nome);

    Optional<Plan> findByNome(String nome);

    List<Plan> findAllByAtivoTrue();
}
