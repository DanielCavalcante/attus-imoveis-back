package com.example.demo.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dtos.PlanCreateDTO;
import com.example.demo.dtos.PlanDTO;
import com.example.demo.dtos.PlanUpdateDTO;
import com.example.demo.exceptions.custom.ConflictException;
import com.example.demo.exceptions.custom.ResourceNotFoundException;
import com.example.demo.model.Plan;
import com.example.demo.repositories.PlanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository repository;

    /** Catalogo publico: so o que esta a venda. */
    @Transactional(readOnly = true)
    public List<PlanDTO> findActive() {
        return repository.findAllByAtivoTrue().stream().map(PlanDTO::fromEntity).toList();
    }

    /** Catalogo interno: inclui inativo, senao o painel nao consegue reativar. */
    @Transactional(readOnly = true)
    public List<PlanDTO> findAll() {
        return repository.findAll().stream().map(PlanDTO::fromEntity).toList();
    }

    @Transactional
    public PlanDTO create(PlanCreateDTO dto) {
        if (repository.existsByNome(dto.nome())) {
            throw new ConflictException("Ja existe plano com este nome");
        }

        Plan plan = new Plan();

        plan.setNome(dto.nome());
        plan.setDescricao(dto.descricao());
        plan.setPrecoMensal(dto.precoMensal());
        plan.setLimiteAnuncios(dto.limiteAnuncios());
        plan.setAtivo(true);

        return PlanDTO.fromEntity(repository.save(plan));
    }

    @Transactional
    public PlanDTO update(Long id, PlanUpdateDTO dto) {
        Plan plan = requireById(id);

        boolean trocouNome = !plan.getNome().equals(dto.nome());

        if (trocouNome && repository.existsByNome(dto.nome())) {
            throw new ConflictException("Ja existe plano com este nome");
        }

        plan.setNome(dto.nome());
        plan.setDescricao(dto.descricao());
        plan.setPrecoMensal(dto.precoMensal());
        plan.setLimiteAnuncios(dto.limiteAnuncios());

        return PlanDTO.fromEntity(repository.save(plan));
    }

    /** Desativa, nunca apaga: assinatura que aponta para o plano ficaria orfa. */
    @Transactional
    public PlanDTO deactivate(Long id) {
        Plan plan = requireById(id);

        plan.setAtivo(false);

        return PlanDTO.fromEntity(repository.save(plan));
    }

    private Plan requireById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plano nao encontrado"));
    }
}
