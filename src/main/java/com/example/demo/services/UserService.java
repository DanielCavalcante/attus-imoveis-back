package com.example.demo.services;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dtos.UserDTO;
import com.example.demo.dtos.UserDetailDTO;
import com.example.demo.dtos.UserListDTO;
import com.example.demo.dtos.UserUpdateDTO;
import com.example.demo.enums.AccountStatus;
import com.example.demo.enums.AccountType;
import com.example.demo.enums.UserRole;
import com.example.demo.exceptions.custom.BusinessException;
import com.example.demo.exceptions.custom.ConflictException;
import com.example.demo.exceptions.custom.ResourceNotFoundException;
import com.example.demo.model.User;
import com.example.demo.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyService companyService;

    @Transactional(readOnly = true)
    public List<UserListDTO> findAll() {
        return repository.findAll().stream().map(this::toListDTO).toList();
    }

    @Transactional(readOnly = true)
    public UserDetailDTO findOne(Long id, String email) {
        return toDetailDTO(daSessao(id, email));
    }

    @Transactional
    public UserDetailDTO create(UserDTO dto) {
        validateAccountType(dto.accountType());
        validatePasswords(dto);
        validateEmail(dto.email());
        validatePhone(dto.phone());
        validateCnpj(dto);

        User entity = new User();

        mapDtoToEntity(dto, entity);
        entity.setPassword(passwordEncoder.encode(dto.password()));
        entity.setAccountType(dto.accountType());
        entity.setCnpj(dto.cnpj());
        entity.setAccountStatus(AccountStatus.ATIVO);

        // O papel interno nao nasce do cadastro publico em hipotese alguma:
        // e ignorado o que vier no corpo. A conta interna vem do bootstrap.
        entity.setRole(UserRole.COMUM);

        User saved = repository.save(entity);

        // O dossie nasce junto da conta, so com o CNPJ: a empresa completa depois.
        if (saved.getAccountType() == AccountType.PJ) {
            companyService.createFor(saved, saved.getCnpj());
        }

        return toDetailDTO(saved);
    }

    @Transactional
    public UserDetailDTO edit(Long id, UserUpdateDTO dto, String email) {
        User entity = daSessao(id, email);

        validateEmailUpdate(dto.email(), entity);
        validatePhoneUpdate(dto.phone(), entity);
        mapUpdateDtoToEntity(dto, entity);

        User saved = repository.save(entity);

        return toDetailDTO(saved);
    }

    @Transactional
    public void delete(Long id, String email) {
        repository.delete(daSessao(id, email));
    }

    /**
     * A conta da sessao, e so ela.
     *
     * O id do caminho nao identifica ninguem: ele e conferido contra a sessao
     * e serve apenas para recusar caminho que nao e o proprio. Enquanto a
     * identidade vinha de fora, cada rota nova precisava lembrar de conferir
     * -- e nenhuma das quatro lembrava. Medido antes desta spec: uma conta
     * comum listava os 14 usuarios, lia o detalhe de outra, gravava nome na
     * conta alheia (200) e a excluia (204).
     *
     * 404 e nao 403 para conta que nao e a sua, pelo mesmo motivo do `doDono`
     * da spec 22: responder "existe, mas nao e seu" conta ao curioso que
     * aquele id existe, e com id sequencial isso enumera a base inteira.
     */
    private User daSessao(Long id, String email) {
        User sessao = repository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (!sessao.getId().equals(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado");
        }

        return sessao;
    }

    private User findUserById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    /** O cadastro publico so abre PF e PJ; INTERNO existe, mas nao por esta porta. */
    private void validateAccountType(AccountType accountType) {
        if (accountType != AccountType.PF && accountType != AccountType.PJ) {
            throw new BusinessException("Tipo de conta deve ser PF ou PJ");
        }
    }

    private void validateCnpj(UserDTO dto) {
        if (dto.accountType() != AccountType.PJ) {
            return;
        }

        if (dto.cnpj() == null || dto.cnpj().isBlank()) {
            throw new BusinessException("CNPJ e obrigatorio para pessoa juridica");
        }

        if (repository.existsByCnpj(dto.cnpj())) {
            throw new ConflictException("CNPJ ja cadastrado");
        }
    }

    private void validateEmail(String email) {
        if (repository.existsByEmail(email)) {
            throw new BusinessException("E-mail já cadastrado");
        }
    }

    private void validatePhone(String phone) {
        if (repository.existsByPhone(phone)) {
            throw new BusinessException("Telefone já cadastrado");
        }
    }

    private void validateEmailUpdate(String email, User user) {         
        boolean emailExists = repository.existsByEmail(email);
        boolean sameEmail = java.util.Objects.equals(email, user.getEmail());

        if (emailExists && !sameEmail) {
            throw new BusinessException("E-mail já cadastrado");
        }
    }

    private void validatePhoneUpdate(String phone, User user) {
        boolean phoneExists = repository.existsByPhone(phone);
        boolean samePhone = java.util.Objects.equals(phone, user.getPhone());

        if (phoneExists && !samePhone) {
            throw new BusinessException("Telefone já cadastrado");
        }
    }

    private void validatePasswords(UserDTO dto) {
        if (!dto.password().equals(dto.confirmPassword())) {
            throw new BusinessException("As senhas não coincidem");
        }
    }

    private void mapDtoToEntity(UserDTO dto, User entity) {
        entity.setFullname(dto.fullname());
        entity.setEmail(dto.email());
        entity.setPhone(dto.phone());
        entity.setPhoto(dto.photo());
    }

    private void mapUpdateDtoToEntity(UserUpdateDTO dto, User entity) {
        entity.setFullname(dto.fullname());
        entity.setEmail(dto.email());
        entity.setPhone(dto.phone());
        entity.setPhoto(dto.photo());
    }

    private UserListDTO toListDTO(User user) {
        return new UserListDTO(user.getId(), user.getFullname(), user.getEmail(), user.getPhone());
    }

    private UserDetailDTO toDetailDTO(User user) {
        return new UserDetailDTO(
            user.getId(),
            user.getFullname(),
            user.getEmail(),
            user.getPhone(),
            user.getPhoto(),
            user.getAccountType(),
            user.getAccountStatus(),
            user.getCnpj()
        );
    }
}