# Veredito — 12-schema-versionado

Base: `00bf5e0..HEAD` mais os não rastreados de `.`.

> **Limitação de independência.** Produzido pela mesma sessão que escreveu o código, a pedido do humano. Leia sabendo que essa garantia não existe aqui.

Nove critérios provados pela suíte sobre Testcontainers (banco novo a cada execução). C10 exige banco preexistente e foi provado contra o ambiente Docker de desenvolvimento, que tinha as sete tabelas criadas por `ddl-auto`.

- **C1** — atendido. `spring.jpa.hibernate.ddl-auto=validate` nas duas configurações, produção e teste, e o teste lê o valor pela própria propriedade injetada, não do arquivo.

- **C2** — atendido. `flyway_schema_history` existe no banco do contêiner de teste, criada pelo Flyway na subida.

- **C3** — atendido. As sete tabelas do domínio existem, consultadas em `pg_tables`.

- **C4, C5, C6** — atendidos. As três restrições únicas nomeadas pelo ciclo (`uk_company_creci`, `uk_favorite_user_announcement`, `uk_view_user_announcement`) existem em `pg_constraint`. São as que o código nomeou; as geradas pelo Hibernate continuam com nome gerado, por decisão declarada.

- **C7** — atendido. Com `validate`, divergência entre entidade e schema derruba a criação do contexto: as onze classes de teste subirem o contexto é a prova. O critério é real, não cerimônia — foi exatamente `validate` que reprovaria um schema extraído errado.

- **C8** — atendido. Conta gravada e relida pelo repositório sobre o schema da migração.

- **C9** — atendido. `flyway_schema_history` tem exatamente uma linha de versão 1 bem-sucedida no banco de teste.

- **C10** — atendido, com percalço registrado abaixo. O banco de desenvolvimento, com 7 usuários, 6 anúncios, 2 empresas e 2 planos criados por `ddl-auto`, foi adotado por baseline na versão 1: `Successfully baselined schema with version: 1` seguido de `Schema "public" is up to date. No migration necessary.` Nenhuma tabela recriada, contagens idênticas antes e depois, API respondendo 200.

## Observações que não são critério

- **A primeira tentativa falhou, e o modo de falhar importa.** Com `baseline-version=0`, o Flyway marcou o schema existente como versão 0 e tentou aplicar a V1 por cima de tabelas já criadas. A migração abortou com `Changes successfully rolled back` e a aplicação não subiu. Nenhum dado foi perdido e nenhum schema ficou pela metade — a transação de migração protegeu o banco, e `validate` teria barrado depois. O schema existente **é** a versão 1, e o baseline passou a refleti-lo.
- Consequência operacional: um banco que já tenha recebido baseline na versão errada precisa da tabela `flyway_schema_history` removida antes de corrigir a configuração. Foi o que se fez aqui. Em produção o Flyway rodará pela primeira vez já com `baseline-version=1`, então o caso não se repete.
- A V1 carrega os nomes de constraint gerados pelo Hibernate (`fk9l5d0...`, `uk6dotk...`). `validate` não olha nome de restrição, então nada quebra; a legibilidade do schema fica pior do que seria se tivessem sido nomeados desde o início.
- `spring.flyway.baseline-on-migrate=true` continua ligado depois de o baseline ter acontecido. Ele só age em schema não vazio sem histórico, então é inerte daqui em diante — mas um banco novo que alguém popule à mão antes da primeira subida seria adotado em silêncio em vez de recusado.
