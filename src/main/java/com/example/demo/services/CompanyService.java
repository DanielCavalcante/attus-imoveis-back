package com.example.demo.services;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dtos.CompanyDetailDTO;
import com.example.demo.dtos.CompanyUpdateDTO;
import com.example.demo.dtos.BackofficeCompanyListDTO;
import com.example.demo.enums.AnalysisStatus;
import com.example.demo.exceptions.custom.BusinessException;
import com.example.demo.exceptions.custom.ConflictException;
import com.example.demo.exceptions.custom.ResourceNotFoundException;
import com.example.demo.model.Company;
import com.example.demo.model.User;
import com.example.demo.repositories.CompanyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository repository;

    /** Chamado na criacao da conta PJ: o dossie nasce so com o CNPJ. */
    @Transactional
    public Company createFor(User user, String cnpj) {
        Company company = new Company();

        company.setUser(user);
        company.setCnpj(cnpj);
        company.setDossieCompleto(false);

        return repository.save(company);
    }

    @Transactional(readOnly = true)
    public CompanyDetailDTO findByAccount(String email) {
        return toDetailDTO(requireCompany(email));
    }

    @Transactional
    public CompanyDetailDTO update(String email, CompanyUpdateDTO dto) {
        Company company = requireCompany(email);

        validateCreci(company, dto);
        apply(dto, company);

        // Gravado agora porque a fila de analise filtra por este campo.
        company.setDossieCompleto(isComplete(company));

        return toDetailDTO(repository.save(company));
    }

    /**
     * Enviar para analise e ato da empresa, nao do backoffice: por isso mora
     * aqui e nao num servico proprio do painel interno.
     */
    @Transactional
    public CompanyDetailDTO submitForAnalysis(String email) {
        Company company = requireCompany(email);

        if (!company.isDossieCompleto()) {
            throw new BusinessException("Complete os dados da empresa antes de enviar para analise");
        }

        if (company.getAnalysisStatus() == AnalysisStatus.APROVADA) {
            throw new BusinessException("Esta empresa ja foi aprovada");
        }

        company.setAnalysisStatus(AnalysisStatus.EM_ANALISE);

        return toDetailDTO(repository.save(company));
    }

    @Transactional(readOnly = true)
    public Page<BackofficeCompanyListDTO> findPendingAnalysis(Pageable pageable) {
        return repository.findAllByAnalysisStatus(AnalysisStatus.EM_ANALISE, pageable)
            .map(this::toListDTO);
    }

    @Transactional
    public CompanyDetailDTO approve(Long companyId, String reviewerEmail) {
        Company company = requireById(companyId);

        company.setAnalysisStatus(AnalysisStatus.APROVADA);
        company.setRejectionReason(null);
        registerDecision(company, reviewerEmail);

        return toDetailDTO(repository.save(company));
    }

    /**
     * Reprovada pode voltar para a fila depois de corrigir: reprovar por erro de
     * digitacao e trancar para sempre transformaria engano em perda de cliente.
     */
    @Transactional
    public CompanyDetailDTO reject(Long companyId, String motivo, String reviewerEmail) {
        Company company = requireById(companyId);

        company.setAnalysisStatus(AnalysisStatus.REPROVADA);
        company.setRejectionReason(motivo);
        registerDecision(company, reviewerEmail);

        return toDetailDTO(repository.save(company));
    }

    private void registerDecision(Company company, String reviewerEmail) {
        company.setReviewedBy(reviewerEmail);
        company.setReviewedAt(LocalDateTime.now());
    }

    private Company requireById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa nao encontrada"));
    }

    private BackofficeCompanyListDTO toListDTO(Company c) {
        return new BackofficeCompanyListDTO(
            c.getId(),
            c.getCnpj(),
            c.getRazaoSocial(),
            c.getNomeFantasia(),
            c.getCreciNumero(),
            c.getCreciUf(),
            c.getResponsavelNome(),
            c.getEmailComercial(),
            c.getAnalysisStatus(),
            c.getReviewedAt()
        );
    }

    /**
     * A conta so alcanca a propria empresa. Nao existe busca por id, entao nao
     * existe a classe de erro de acessar dossie alheio trocando um numero na URL.
     */
    private Company requireCompany(String email) {
        return repository.findByUserEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Esta conta nao tem empresa associada"));
    }

    /** Duas empresas com o mesmo CRECI significa que uma delas mente. A spec 05 apura. */
    private void validateCreci(Company company, CompanyUpdateDTO dto) {
        boolean mesmoCreci = dto.creciNumero().equals(company.getCreciNumero())
            && dto.creciUf().equals(company.getCreciUf());

        if (mesmoCreci) {
            return;
        }

        if (repository.existsByCreciNumeroAndCreciUf(dto.creciNumero(), dto.creciUf())) {
            throw new ConflictException("CRECI ja cadastrado por outra empresa");
        }
    }

    private void apply(CompanyUpdateDTO dto, Company company) {
        company.setRazaoSocial(dto.razaoSocial());
        company.setNomeFantasia(dto.nomeFantasia());
        company.setCreciNumero(dto.creciNumero());
        company.setCreciUf(dto.creciUf());
        company.setResponsavelNome(dto.responsavelNome());
        company.setResponsavelCpf(dto.responsavelCpf());
        company.setTelefoneComercial(dto.telefoneComercial());
        company.setEmailComercial(dto.emailComercial());
        company.setCep(dto.cep());
        company.setLogradouro(dto.logradouro());
        company.setNumero(dto.numero());
        company.setComplemento(dto.complemento());
        company.setCidade(dto.cidade());
        company.setUf(dto.uf());
    }

    /** Complemento e o unico campo que nao conta: endereco sem complemento existe. */
    private boolean isComplete(Company c) {
        return filled(c.getRazaoSocial())
            && filled(c.getNomeFantasia())
            && filled(c.getCreciNumero())
            && filled(c.getCreciUf())
            && filled(c.getResponsavelNome())
            && filled(c.getResponsavelCpf())
            && filled(c.getTelefoneComercial())
            && filled(c.getEmailComercial())
            && filled(c.getCep())
            && filled(c.getLogradouro())
            && filled(c.getNumero())
            && filled(c.getCidade())
            && filled(c.getUf());
    }

    private boolean filled(String value) {
        return value != null && !value.isBlank();
    }

    private CompanyDetailDTO toDetailDTO(Company c) {
        return new CompanyDetailDTO(
            c.getId(),
            c.getCnpj(),
            c.getRazaoSocial(),
            c.getNomeFantasia(),
            c.getCreciNumero(),
            c.getCreciUf(),
            c.getResponsavelNome(),
            c.getResponsavelCpf(),
            c.getTelefoneComercial(),
            c.getEmailComercial(),
            c.getCep(),
            c.getLogradouro(),
            c.getNumero(),
            c.getComplemento(),
            c.getCidade(),
            c.getUf(),
            c.isDossieCompleto(),
            c.getAnalysisStatus(),
            c.getRejectionReason()
        );
    }
}
