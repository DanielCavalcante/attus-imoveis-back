package com.example.demo.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Neighborhood;

@Repository
public interface NeighborhoodRepository extends JpaRepository<Neighborhood, Long> {

    /**
     * O catalogo de uma cidade, em ordem alfabetica.
     *
     * Ordena pela forma normalizada, nao pelo nome cru: `order by n.name` usa a
     * colacao do banco, e sob colacao C o acento manda "Água Fria" para depois
     * de "Varjão" -- ordem que nenhum leitor de portugues reconhece. Sem acento
     * e em minuscula, a ordem e a mesma em qualquer colacao.
     *
     * A ordenacao vem do banco e nao da memoria: e ela que a tela apresenta, e
     * ordenar depois exigiria que todo consumidor lembrasse de fazer o mesmo.
     */
    @Query("""
        select n.name from Neighborhood n
        where n.citySearch = :cidade
        order by n.nameSearch
        """)
    List<String> nomesDe(@Param("cidade") String cidade);
}
