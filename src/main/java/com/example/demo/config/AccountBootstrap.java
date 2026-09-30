package com.example.demo.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.AccountStatus;
import com.example.demo.enums.AccountType;
import com.example.demo.enums.UserRole;
import com.example.demo.model.User;
import com.example.demo.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Carga de contas. Roda a cada boot e e idempotente: so toca conta sem tipo.
 *
 * Nao e SQL em src/main/resources porque a conta interna precisa de senha
 * codificada em BCrypt, que o script nao produz.
 */
@Component
@RequiredArgsConstructor
public class AccountBootstrap implements ApplicationRunner {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Value("${attus.bootstrap.internal.email:}")
    private String internalEmail;

    @Value("${attus.bootstrap.internal.password:}")
    private String internalPassword;

    @Value("${attus.bootstrap.internal.phone:}")
    private String internalPhone;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        backfillExistingAccounts();
        createInternalAccount();
    }

    /** Conta gravada antes desta spec nao tem tipo: era anterior a distincao PF/PJ. */
    public void backfillExistingAccounts() {
        // Consulta em vez de trazer a tabela e filtrar em Java: isto roda a cada
        // boot, inclusive em cada contexto Spring levantado pela suite.
        List<User> pending = repository.findAllByAccountTypeIsNull();

        pending.forEach(user -> {
            user.setAccountType(AccountType.PF);
            user.setAccountStatus(AccountStatus.ATIVO);
            user.setRole(UserRole.COMUM);
        });

        repository.saveAll(pending);
    }

    /**
     * Sem as variaveis de ambiente, nenhuma conta interna e criada. O repositorio
     * nao carrega credencial interna padrao.
     */
    public void createInternalAccount() {
        boolean configured = !internalEmail.isBlank() && !internalPassword.isBlank();

        if (!configured || repository.existsByEmail(internalEmail)) {
            return;
        }

        User internal = new User();

        internal.setFullname("Equipe interna");
        internal.setEmail(internalEmail);
        internal.setPhone(internalPhone.isBlank() ? internalEmail : internalPhone);
        internal.setPassword(passwordEncoder.encode(internalPassword));
        internal.setAccountType(AccountType.INTERNO);
        internal.setAccountStatus(AccountStatus.ATIVO);
        internal.setRole(UserRole.INTERNO);

        repository.save(internal);
    }
}
