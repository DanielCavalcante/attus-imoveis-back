package com.example.demo.services;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dtos.UserDTO;
import com.example.demo.dtos.UserDetailDTO;
import com.example.demo.dtos.UserListDTO;
import com.example.demo.exceptions.custom.BusinessException;
import com.example.demo.exceptions.custom.ResourceNotFoundException;
import com.example.demo.model.User;
import com.example.demo.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserListDTO> findAll() {
        return repository.findAll().stream().map(this::toListDTO).toList();
    }

    @Transactional(readOnly = true)
    public UserDetailDTO findOne(Long id) { 
        User user = findUserById(id);
        return toDetailDTO(user);
    }

    @Transactional
    public UserDetailDTO create(UserDTO dto) {
        validatePasswords(dto);
        validateEmail(dto.email());
        validatePhone(dto.phone());

        User entity = new User();

        mapDtoToEntity(dto, entity);
        entity.setPassword(passwordEncoder.encode(dto.password()));

        User saved = repository.save(entity);

        return toDetailDTO(saved);
    }

    @Transactional
    public UserDetailDTO edit(Long id, UserDTO dto) {
        User entity = findUserById(id);

        validatePasswords(dto);
        validateEmailUpdate(dto.email(), entity);
        validatePhoneUpdate(dto.phone(), entity);

        mapDtoToEntity(dto, entity);
        entity.setPassword(passwordEncoder.encode(dto.password()));

        User saved = repository.save(entity);

        return toDetailDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        User entity = findUserById(id);
        repository.delete(entity);
    }

    private User findUserById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
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
        boolean sameEmail = email.equals(user.getEmail());

        if (emailExists && !sameEmail) {
            throw new BusinessException("E-mail já cadastrado");
        }
    }

    private void validatePhoneUpdate(String phone, User user) {
        boolean phoneExists = repository.existsByPhone(phone);
        boolean samePhone = phone.equals(user.getPhone());

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

    private UserListDTO toListDTO(User user) {
        return new UserListDTO(user.getId(), user.getFullname(), user.getEmail(), user.getPhone());
    }

    private UserDetailDTO toDetailDTO(User user) {
        return new UserDetailDTO(user.getId(), user.getFullname(), user.getEmail(), user.getPhone(), user.getPhoto());
    }
}