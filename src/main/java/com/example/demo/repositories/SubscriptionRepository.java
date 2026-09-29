package com.example.demo.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Subscription;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByCompanyIdAndVigenteTrue(Long companyId);

    /**
     * O vinculo vigente da conta, com o plano junto.
     *
     * `join fetch` de proposito: o teto e lido a cada publicacao, e sem ele
     * cada leitura vira duas consultas para responder uma pergunta so.
     */
    @Query("""
        select s from Subscription s
        join fetch s.plan
        where s.company.user.email = :email and s.vigente = true
        """)
    Optional<Subscription> vigenteDe(@Param("email") String email);
}
