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

    /**
     * As empresas aprovadas, com o plano vigente e o que elas tem no ar.
     *
     * Numa consulta so, com a contagem por subconsulta: buscar os anuncios de
     * cada linha depois seria uma ida ao banco por empresa, e esta e
     * justamente a tela que lista todas.
     *
     * `left join`: empresa aprovada e ainda sem plano precisa aparecer -- ela
     * e o caso que a tela existe para resolver.
     */
    @Query(value = """
        select new com.example.demo.dtos.CompanyPlanListDTO(
            c.id, c.razaoSocial, c.nomeFantasia, c.cnpj,
            p.nome, p.limiteAnuncios,
            (select count(a) from Announcement a where a.user = c.user and a.active = true))
        from Company c
        left join Subscription s on s.company = c and s.vigente = true
        left join s.plan p
        where c.analysisStatus = com.example.demo.enums.AnalysisStatus.APROVADA
        """,
        countQuery = """
        select count(c) from Company c
        where c.analysisStatus = com.example.demo.enums.AnalysisStatus.APROVADA
        """)
    org.springframework.data.domain.Page<com.example.demo.dtos.CompanyPlanListDTO> empresasAprovadas(
        org.springframework.data.domain.Pageable pageable
    );
}
