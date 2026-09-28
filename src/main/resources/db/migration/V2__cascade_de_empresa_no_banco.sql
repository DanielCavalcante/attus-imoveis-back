-- Remoção da empresa junto com a conta passa a ser do banco, nao do JPA.
--
-- O mapeamento @OneToOne(mappedBy) em User existia so para cascatear a remocao.
-- O lado inverso de um @OneToOne nao pode ser lazy sem bytecode enhancement:
-- o Hibernate precisa consultar para saber se poe null ou proxy. O efeito era
-- uma consulta a companies por conta carregada -- inclusive em
-- loadUserByUsername, ou seja, em TODA requisicao autenticada.
--
-- Com ON DELETE CASCADE o banco resolve o ciclo de vida e o mapeamento some.

ALTER TABLE companies DROP CONSTRAINT fk9l5d0fem75e59uwf9upwuf9du;

ALTER TABLE companies
    ADD CONSTRAINT fk_company_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE;
