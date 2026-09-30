# 14-paginacao-e-consultas-das-filas

## Intenção
As filas do backoffice passam a ter ordem de atendimento e tamanho previsível, e param de disparar consulta por linha.

## Depende de
09-apoio-de-venda

> **Aprovada no gate humano em 2026-09-29.** Todos os critérios aceitos.

## Critérios
- [x] **C1** `contrato-api` — `GET /api/v1/backoffice/companies` aceita `page` e `size`.
- [x] **C2** `contrato-api` — A resposta da fila de empresas traz o total de elementos.
- [x] **C3** `dominio` — Com três empresas na fila e `size=2`, a primeira página traz duas.
- [x] **C4** `dominio` — Com três empresas na fila e `size=2`, a segunda página traz a restante.
- [x] **C5** `dominio` — A fila de empresas vem da mais antiga para a mais nova.
- [x] **C6** `contrato-api` — `GET /api/v1/backoffice/support` aceita `page` e `size`.
- [x] **C7** `dominio` — A fila de apoio vem da mais antiga para a mais nova.
- [x] **C8** `performance` — A fila de apoio com cinco pedidos é montada com no máximo três consultas ao banco.
- [x] **C9** `dominio` — A carga de contas seleciona apenas contas sem tipo.
- [x] **C10** `performance` — A carga com cinco contas tipadas e uma sem tipo carrega uma linha.
- [x] **C11** `performance` — Carregar uma conta pelo e-mail dispara no máximo uma consulta.
- [x] **C12** `persistencia` — Remover a conta remove a empresa dela.

## Contrato técnico
- **Ordem de atendimento é da mais antiga para a mais nova.** Duas imobiliárias esperando e ninguém sabendo quem chegou primeiro é o defeito que a homologação apontou. A fila de empresas ordena por `id` crescente, porque `Company` não tem instante de criação; a de apoio ordena por `createdAt`.
- Tamanho padrão de página: 20. `Pageable` do Spring Data, com `page` e `size` na consulta.
- A resposta passa a ser `Page`, não lista. **Isto quebra o contrato com o painel do frontend**, que hoje espera array — a spec seguinte, no outro repositório, acompanha.
- O N+1 da fila de apoio se resolve com `join fetch` explícito nas duas associações `@ManyToOne`. Com `@EntityGraph` a medição continuou em seis consultas para cinco pedidos; o `join fetch` traz tudo em uma. Associação para-um não multiplica linha, então a paginação continua acontecendo no SQL.
- **Descoberta ao medir, e maior que o alvo original:** `User` tinha `@OneToOne(mappedBy = "user")` para `Company`, acrescentado na spec 02 só para cascatear a remoção. O lado inverso de um `@OneToOne` não pode ser lazy sem bytecode enhancement — o Hibernate precisa consultar para saber se põe nulo ou proxy. O efeito era **uma consulta a `companies` por conta carregada, inclusive em `loadUserByUsername`, ou seja, em toda requisição autenticada.** O mapeamento sai e o ciclo de vida passa para `ON DELETE CASCADE` no banco, via `V2`.
- `V2__cascade_de_empresa_no_banco.sql` é a primeira migração sob a disciplina da spec 12: mudança de entidade agora exige arquivo versionado.
- A carga de contas passa a consultar `findAllByAccountTypeIsNull` em vez de trazer a tabela e filtrar em Java. Ela roda a cada boot, inclusive em cada contexto da suíte.
- Contagem de consultas medida pelas estatísticas do Hibernate, ligadas só no perfil de teste. O número no critério é o que separa `performance` de desejo.

## Fora de escopo
- Paginar `GET /api/v1/announcements`, favoritos e histórico. São o caminho público e quente, o contrato deles atinge home, listagem, dashboard e duas telas de conta, e a mudança merece spec própria nos dois repositórios. **Continua aberto**, e é a observação de volume mais consequente da homologação.
- Filtro por estado, busca por CNPJ ou por cidade nas filas.
- Cursor em vez de deslocamento: com o volume previsto, `offset` basta, e trocar depois é barato.
- Índice em `analysis_status` e em `status`: sem volume medido, seria adivinhação.

## Plano
1. `repositories/CompanyRepository.java` — `Page<Company> findAllByAnalysisStatus(..., Pageable)`.
2. `repositories/SupportRequestRepository.java` — `findAllByStatus(..., Pageable)` com `@EntityGraph`.
3. `repositories/UserRepository.java` — `findAllByAccountTypeIsNull`.
4. `services/CompanyService.java` e `services/SupportRequestService.java` — recebem `Pageable`.
5. `config/AccountBootstrap.java` — consulta em vez de filtro em memória.
6. `controllers/BackofficeController.java` — `Pageable` nas duas filas.
7. `src/test/resources/application.properties` — estatísticas do Hibernate.
8. `src/test/java/com/example/demo/PaginacaoDasFilasSpecTest.java`.

Fatia única: os critérios de `performance` dependem das consultas novas existirem.

## Perguntas em aberto
nenhuma
