# 12-schema-versionado

## Intenção
O schema do banco passa a ser um arquivo versionado, reproduzível e revisável — hoje ele é o que o Hibernate resolveu criar em cada boot, e não existe em lugar nenhum do repositório.

## Depende de
11-correcoes-de-seguranca

> **Aprovada no gate humano em 2026-09-29.** Todos os critérios aceitos.

## Critérios
- [x] **C1** `persistencia` — `spring.jpa.hibernate.ddl-auto` vale `validate`.
- [x] **C2** `persistencia` — Com banco vazio, a aplicação sobe e a tabela `flyway_schema_history` existe.
- [x] **C3** `persistencia` — Após a migração, as sete tabelas do domínio existem.
- [x] **C4** `persistencia` — Após a migração, a restrição única do par CRECI existe.
- [x] **C5** `persistencia` — Após a migração, a restrição única do par conta/anúncio de favoritos existe.
- [x] **C6** `persistencia` — Após a migração, a restrição única do par conta/anúncio de visualizações existe.
- [x] **C7** `persistencia` — A aplicação sobe com `validate`, ou seja, o mapeamento JPA confere com o schema da migração.
- [x] **C8** `persistencia` — Uma conta gravada e relida atravessa o schema versionado.
- [x] **C9** `persistencia` — O histórico do Flyway registra exatamente uma aplicação bem-sucedida da versão 1.
- [x] **C10** `persistencia` — Banco já criado por `ddl-auto` é adotado por baseline, sem recriar tabela.

## Contrato técnico
- Flyway, que já vem gerenciado pelo parent do Spring Boot. Com Postgres 16 é preciso também `flyway-database-postgresql`, separado do core desde o Flyway 10.
- `V1__schema_inicial.sql` foi **extraído do banco real** com `pg_dump --schema-only`, não escrito à mão. O objetivo é descrever exatamente o que as specs 01 a 11 produziram; um schema redigido do zero divergiria em detalhe e faria `validate` reprovar na subida.
- Os nomes gerados pelo Hibernate (`fk9l5d0...`, `uk6dotk...`) ficam como estão. Renomeá-los agora exigiria migração de banco existente para ganhar só legibilidade, e o `validate` não olha nome de constraint.
- `spring.flyway.baseline-on-migrate=true`: o banco de desenvolvimento já tem as sete tabelas criadas por `ddl-auto`, e sem baseline o Flyway recusaria migrar um schema não vazio. Em banco novo o baseline não tem efeito.
- **`ddl-auto` passa a `validate`, não a `none`.** `validate` falha a subida quando entidade e schema divergem — é exatamente o alarme que faltava, e é barato.
- A suíte usa Testcontainers com banco novo por execução, então lá o Flyway cria tudo do zero. A configuração de teste passa a espelhar a de produção: mesma migração, mesmo `validate`.
- Daqui em diante, **toda mudança de entidade exige um `V<n>__*.sql`**. Nenhuma coluna nasce de `ddl-auto` outra vez.

## Fora de escopo
- Reescrever os nomes de constraint gerados pelo Hibernate.
- Migração de dado (backfill, transformação): a V1 só descreve estrutura, porque é o que existe.
- Rollback automático de migração: Flyway Community não tem `undo`, e fingir que tem é pior que não ter.
- `AccountBootstrap` continua como está: ele povoa dado, não estrutura.
- Paginação, handlers de erro e as demais pendências da homologação — cada uma na sua spec.

## Plano
1. `pom.xml` — `flyway-core` e `flyway-database-postgresql`.
2. `src/main/resources/db/migration/V1__schema_inicial.sql` — schema extraído do banco real.
3. `src/main/resources/application.properties` — `ddl-auto=validate`, Flyway ligado, baseline ligado.
4. `src/test/resources/application.properties` — mesma configuração.
5. `src/test/java/com/example/demo/SchemaVersionadoSpecTest.java` — consulta o catálogo do Postgres para provar tabela, restrição e histórico.

Fatia única: todos os critérios dependem da migração existir e da configuração apontar para ela.

## Perguntas em aberto
nenhuma
