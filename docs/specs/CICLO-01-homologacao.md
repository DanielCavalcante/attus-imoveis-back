# CICLO 01 — Leitura arquitetural de homologação

Alvos: `attus-imoveis-back` (base `1e03b12`) e `attus-imoveis-front` (base `aa69704`),
com os arquivos não rastreados de ambas as árvores.

Nove specs: 01, 02, 05, 06, 07, 09, 10 (back); 03, 08 (front).

Caminhos do back são relativos a `src/main/java/com/example/demo/`, salvo quando
indicado. Caminhos do front são relativos à raiz de `attus-imoveis-front/`.

Cada observação está marcada como **JÁ REGISTRADA** (aparece em algum `-veredito.md`)
ou **NÃO REGISTRADA**. São observações, não recomendações.

---

## 1. Esta decisão segura se o volume triplicar?

1. `AccountBootstrap.backfillExistingAccounts` carrega a tabela `users` inteira em
   memória e filtra em Java — `repository.findAll().stream().filter(...)`
   (`config/AccountBootstrap.java:51-53`) —, e roda a cada boot da aplicação
   (`config/AccountBootstrap.java:44-47`), inclusive a cada contexto Spring
   levantado na suíte. **NÃO REGISTRADA** (o veredito 01 C9 confirma que a carga
   funciona; não observa como ela lê).

2. `BackofficeSupportListDTO.fromEntity` desreferencia `r.getAnnouncement()` e
   `r.getUser()` para cada linha (`dtos/BackofficeSupportListDTO.java:22-28`),
   sobre a lista de `findAllByStatus` (`services/SupportRequestService.java:62`),
   e as duas associações são `FetchType.LAZY`
   (`model/SupportRequest.java:29,33`): duas consultas adicionais por pedido na
   fila. **NÃO REGISTRADA** (o veredito 09 registra a ausência de paginação da
   mesma fila, não o número de consultas por linha).

3. `UserLibraryService.registerView` é uma escrita síncrona dentro de
   `GET /api/v1/announcements/{id}` (`controllers/AnnouncementController.java:51-54`
   → `services/UserLibraryService.java:70-90`): toda abertura de detalhe por conta
   autenticada custa um `select` mais um `insert`/`update` no caminho da
   requisição, na rota pública mais quente. **JÁ REGISTRADA** (veredito 10,
   primeira observação fora de critério).

4. `CompanyService.findPendingAnalysis` devolve a fila inteira, sem paginação nem
   ordenação (`services/CompanyService.java:80-85`), e
   `SupportRequestService.findOpenRequests` faz o mesmo
   (`services/SupportRequestService.java:61-66`). **JÁ REGISTRADA** (vereditos 05 e
   09).

5. `AnnouncementService.findAll` carrega a tabela inteira de anúncios
   (`services/AnnouncementService.java:43`) e alimenta `GET /api/v1/announcements`,
   liberada sem autenticação (`config/SecurityConfig.java:46`). É anterior ao
   ciclo, mas nenhum veredito do ciclo a menciona, enquanto quatro entidades novas
   foram penduradas em `Announcement`. **NÃO REGISTRADA**.

6. `UserLibraryService.history` devolve o histórico inteiro da conta
   (`services/UserLibraryService.java:93-98`, consulta em
   `repositories/AnnouncementViewRepository.java:18-23`), sem limite de linhas nem
   expurgo por idade. **JÁ REGISTRADA** (veredito 10, terceira observação).

---

## 2. Algum acoplamento novo preocupa a longo prazo?

1. `UserService` passou a depender de `CompanyService`
   (`services/UserService.java:30`) e a criação de conta executa a criação do
   dossiê na mesma transação (`services/UserService.java:66-68`). O cadastro
   público de usuário deixou de ser independente do domínio de empresa.
   **NÃO REGISTRADA**.

2. `AnnouncementController` passou a agregar três serviços — `AnnouncementService`,
   `SupportRequestService`, `UserLibraryService`
   (`controllers/AnnouncementController.java:33-35`) — e a leitura pública de
   anúncio (`:43-57`) agora depende de `UserLibraryService`. **NÃO REGISTRADA**
   enquanto acoplamento (o veredito 10 registra o custo da escrita, não a
   dependência do controller de leitura).

3. A autorização de papel vem da claim do token, sem ida ao banco
   (`security/JwtAuthenticationFilter.java:98-111`), e o token vive uma hora
   (`services/JwtService.java:48-50`): remover o papel `INTERNO` de uma conta não
   tem efeito até a expiração. **NÃO REGISTRADA** no back (o veredito 03 do front
   registra o efeito análogo de `accountType` na navegação, não a autorização de
   papel no servidor).

4. Front: `BASE_URL = "http://localhost:8080/api/v1"` é literal no código
   (`app/services/api.ts:1`), enquanto o app passou a ser publicado em container
   (`docker-compose.yml:46-54` do back). Toda chamada nova do ciclo
   (`app/services/company.service.ts`, `app/services/backoffice.service.ts`) herda
   esse literal. **NÃO REGISTRADA**.

5. Front: `app/types/backoffice.ts:7-28` e `app/schemas/company.ts:7-22` replicam à
   mão a forma de `BackofficeCompanyListDTO`, `PlanDTO` e `CompanyUpdateDTO`, e a
   tela de planos consome o campo derivado `anunciosIlimitados`
   (`app/portal/backoffice/planos/page.tsx:128`), calculado no servidor em
   `PlanDTO.fromEntity`. Não há verificação de contrato entre os dois lados: os
   testes do front exercitam só o schema e o montador de corpo
   (`app/services/__tests__/user-payload.test.ts`,
   `app/schemas/__tests__/user-schema.test.ts`). **NÃO REGISTRADA**.

6. O ciclo de vida das cinco entidades novas passou a ser resolvido por cascade
   JPA: `Announcement` acumula três coleções `cascade = ALL, orphanRemoval = true`
   (`model/Announcement.java:115-122`) e `User` mais duas
   (`model/User.java:71,78`). **PARCIALMENTE REGISTRADA** — o veredito 09 registra
   a regressão de chave estrangeira que motivou o primeiro cascade; nenhum veredito
   observa que a decisão virou o padrão para toda entidade nova.

---

## 3. A implementação diverge do plano declarado em alguma spec, sem que o veredito tenha sinalizado?

1. Spec 01, contrato técnico: *"A primeira conta `INTERNO` é criada pelo script de
   carga a partir de variáveis de ambiente... Não existe credencial interna padrão
   no repositório."* `docker-compose.yml:40-42` traz
   `interno@attus.dev` / `InternoDev123` como valores default literais no arquivo
   versionado. O veredito 01 afirma o contrário (`"O repositorio nao carrega
   credencial interna padrao"`), e a spec 08 declarou os valores de
   desenvolvimento sem reconciliar a contradição. **NÃO REGISTRADA**.

2. Spec 01, plano, item 3: *"Script de carga em `src/main/resources/`"*. Foi
   implementado como `ApplicationRunner` Java em `config/AccountBootstrap.java:28`,
   com a justificativa no próprio arquivo (`:20-25`). O veredito 01 C9 avalia o
   comportamento e não sinaliza a troca de mecanismo e de local. **NÃO REGISTRADA**.

3. Spec 03, contrato técnico: *"O tipo de conta vem do JWT... `auth-context.tsx`
   passa a lê-la."* Na restauração de sessão, `app/contexts/auth-context.tsx:74-80`
   prefere o JSON gravado em `localStorage["user"]` e só decodifica o token quando
   esse item não existe (`:81-85`). `accountType` e `role` da sessão vêm, no caso
   comum, de um blob editável pelo visitante, não do token assinado — e são
   exatamente os campos que decidem `menu.tsx` e `InternalRoute`
   (`app/components/internal-route.tsx:42`). O veredito 03 afirma *"A sessão lê
   `accountType` do JWT"*. **NÃO REGISTRADA**.

4. Spec 08, plano, item 5: quatro arquivos de hook
   (`use-pending-companies.ts`, `use-review-company.ts`, `use-plans.ts`,
   `use-plan-mutations.ts`). Tudo foi entregue em um arquivo,
   `app/hooks/use-backoffice.ts:18-70`. **NÃO REGISTRADA**.

5. Spec 10, C15 — *"Remover um anúncio remove os favoritos e o histórico dele"* — é
   dado como atendido pelo cascade (`model/Announcement.java:118-122`), mas não
   existe rota que remova um anúncio: o `@DeleteMapping` está comentado
   (`controllers/AnnouncementController.java:115-118`), fato que a própria spec 06
   registra no fora de escopo. O critério só é alcançável via cascade de
   `DELETE /api/v1/users/{id}`. O veredito 10 C15 não observa isso.
   **NÃO REGISTRADA**.

6. Spec 09, contrato técnico: *"Entidade `SupportRequest`: anúncio, **solicitante**..."*.
   `SupportRequestService.open` grava `request.setUser(announcement.getUser())`
   (`services/SupportRequestService.java:43`) — o dono do anúncio, não o principal
   autenticado. Hoje coincidem porque `requireOwnedAnnouncement` (`:84-93`) já
   exigiu posse; o campo gravado, porém, não é o que o nome declara, e o veredito
   09 C5 valida comparando justamente os ids do dono. **NÃO REGISTRADA**.

---

## 4. Há dívida sendo criada conscientemente? Foi registrada, ou está silenciosa?

1. O schema continua evoluindo por `spring.jpa.hibernate.ddl-auto=update`
   (`src/main/resources/application.properties:8`), enquanto o ciclo criou cinco
   tabelas (`companies`, `plans`, `support_requests`, `favorites`,
   `announcement_views`) e quatro constraints únicas novas. A spec 01 cita
   `ddl-auto=update` apenas como razão para existir a carga; nenhuma migração
   versionada foi introduzida e nenhum veredito registra isso como dívida.
   **NÃO REGISTRADA**.

2. `GlobalExceptionHandler` tem sete handlers
   (`exceptions/handler/GlobalExceptionHandler.java:25,32,40,47,55,68,76`) e nenhum
   para `DataIntegrityViolationException`, enquanto o ciclo acrescentou as
   constraints `uk_company_creci` (`model/Company.java:30-33`), `plans.nome`
   (`services/PlanService.java:38`), `uk_favorite_user_announcement`
   (`model/Favorite.java:20-23`) e `uk_view_user_announcement`
   (`model/AnnouncementView.java:24-27`). Toda corrida nessas quatro responde 500.
   **PARCIALMENTE REGISTRADA** — o veredito 01 C8 cita o caso do CNPJ; as quatro
   constraints novas do ciclo não aparecem em veredito algum.

3. `GET /api/v1/announcements/me` continua público — o `permitAll` de
   `/api/v1/announcements/*` (`config/SecurityConfig.java:47`) casa com `/me`, e o
   método chama `userDetails.getUsername()` sem guarda
   (`controllers/AnnouncementController.java:150-151`), respondendo 500 em vez de
   403. **JÁ REGISTRADA** (veredito 06, primeira observação) — e permanece aberta
   ao fim do ciclo, sem item que a feche.

4. O CNPJ vive em dois lugares: `User.cnpj` (`model/User.java:64`, único no banco) e
   `Company.cnpj` (`model/Company.java:45-46`), copiado na criação
   (`services/UserService.java:67`, `services/CompanyService.java:34`). Não existe
   caminho de edição que os mantenha em sincronia. **JÁ REGISTRADA** (veredito 02,
   primeira observação).

5. `Dockerfile:5-6` do front roda `npm install` a partir de `package.json` apenas,
   com o comentário *"Não há lockfile commitado neste repo (yarn.lock foi
   removido)"* — mas `package-lock.json` passou a existir na árvore neste ciclo
   (arquivo não rastreado na raiz do front). O build de container não usa o
   lockfile que o ciclo produziu. **NÃO REGISTRADA**.

6. A suíte do back compartilha um único container Postgres estático, iniciado em
   bloco `static` e nunca reiniciado
   (`src/test/java/com/example/demo/support/PostgresIntegrationTest.java:16-21`),
   com `ddl-auto=update` (`src/test/resources/application.properties:1`) e sem
   limpeza declarada entre as oito classes de spec. `AccountBootstrap` roda a cada
   contexto sobre o estado acumulado. **NÃO REGISTRADA**.
