# 01-contas-tipadas

## Intenção
Quem se cadastra passa a declarar se é pessoa física ou jurídica, e a conta carrega tipo, status e papel — hoje todo cadastro nasce idêntico e sem distinção alguma.

## Depende de
nenhuma

## Critérios
- [ ] **C1** `contrato-api` — `POST /api/v1/users` sem o campo de tipo de conta é recusado com 400 e a mensagem nomeia o campo.
- [ ] **C2** `contrato-api` — `POST /api/v1/users` com tipo de conta fora do conjunto aceito é recusado com 400.
- [ ] **C3** `dominio` — Cadastro declarado como pessoa física persiste a conta com tipo `PF`.
- [ ] **C4** `dominio` — Cadastro declarado como pessoa jurídica persiste a conta com tipo `PJ`.
- [ ] **C5** `dominio` — Conta criada com tipo `PF` nasce com status `ATIVO`.
- [ ] **C6** `dominio` — Conta criada com tipo `PJ` nasce com status `ATIVO`.
- [ ] **C7** `dominio` — Cadastro de pessoa jurídica sem CNPJ é recusado com 400.
- [ ] **C8** `persistencia` — Cadastro com CNPJ já existente é recusado com 409.
- [ ] **C9** `persistencia` — Após a carga, toda conta que já existia no banco tem status `ATIVO`.
- [ ] **C10** `seguranca` — O JWT emitido carrega o tipo de conta numa claim.
- [ ] **C11** `seguranca` — O JWT emitido carrega o papel numa claim.
- [ ] **C12** `seguranca` — Requisição a rota sob `/api/v1/backoffice/**` com JWT de papel diferente de `INTERNO` responde 403.
- [ ] **C13** `seguranca` — Requisição a rota sob `/api/v1/backoffice/**` com JWT de papel `INTERNO` não responde 403.
- [ ] **C14** `seguranca` — Nenhum corpo enviado a `POST /api/v1/users` produz conta com papel `INTERNO`.
- [ ] **C15** `contrato-api` — `GET /api/v1/announcements` sem autenticação continua respondendo 200.

## Contrato técnico
- O conjunto de tipos é `PF`, `PJ`, `INTERNO`. Não existe tipo "usuário final": quem só navega é uma `PF` que ainda não anunciou.
- **Tipo de conta é identidade, não capacidade.** Navegar, favoritar, contatar anunciante e anunciar valem para `PF` e `PJ`; o tipo decide a regra que incide sobre o ato (quota na `PF`, plano na `PJ`), nunca se o ato existe. Nenhuma autorização pode ser escrita contra `accountType` — o que gateia é status, papel e, nas specs futuras, quota e plano.
- Tipo de conta, status e papel são enums persistidos como `EnumType.STRING`, seguindo `PropertyType` e `ReasonType`.
- Papel é atributo da conta, não tabela de junção: esta spec não introduz papel múltiplo.
- A claim do papel entra no JWT emitido por `JwtService`. ~~`JwtAuthenticationFilter` popula as authorities a partir dela, sem consulta extra ao banco.~~ **Emendado pela spec 11:** a justificativa era falsa — o filtro já chamava `loadUserByUsername` em toda requisição, então a claim não evitava consulta alguma, só impedia revogar acesso antes da expiração. As authorities passaram a vir do `UserDetails` carregado. A claim segue no token, como dica para a interface.
- O prefixo `/api/v1/backoffice/**` é reservado aqui e liberado por papel em `SecurityConfig`; as rotas de backoffice em si são de outra spec, então basta uma rota de verificação viva.
- A primeira conta `INTERNO` é criada pelo script de carga a partir de variáveis de ambiente; ausentes, o script não cria conta alguma. Não existe credencial interna padrão no repositório.
- Nenhum status bloqueia autenticação. **Emenda de 2026-09-28:** a conta nasce `ATIVO` porque o serviço de e-mail foi adiado; sem ele, nascer pendente trancaria todo cadastro novo sem existir chave. `PENDENTE_EMAIL` continua no enum, reservado, e volta a ser o status inicial quando a confirmação por e-mail entrar.
- `spring.jpa.hibernate.ddl-auto=update` cria coluna mas não preenche linha existente: sem o script de carga, as contas já gravadas ficam com status nulo e C9 reprova.

## Fora de escopo
- Envio e confirmação de e-mail e o bloqueio de login de conta não ativa — adiados por decisão do gate; o provedor de e-mail ainda não foi escolhido.
- Fila e tela de análise de confiabilidade — spec 05; aqui o status de PJ apenas nasce.
- Planos, pagamento e quota de anúncios — dependem de conta tipada, não o contrário.
- Favoritos, histórico e contato com anunciante — spec própria; nenhum critério aqui depende deles.
- CPF da pessoa física — nenhum critério desta spec depende de documento de PF. Se o negócio exigir documento, vira spec própria em vez de inchar esta.
- Validação de dígito verificador de CNPJ — a spec 05 apura a empresa de verdade; aqui CNPJ é só obrigatório e único.
- Qualquer tela: a escolha de tipo no formulário é da spec 03, no frontend.

## Plano
1. `enums/AccountType.java`, `enums/AccountStatus.java`, `enums/UserRole.java`.
2. `model/User.java` — campos `accountType`, `accountStatus`, `role`, `cnpj`.
3. Script de carga em `src/main/resources/` — marca conta existente como `PF`, `ATIVO`, papel comum, `cnpj` nulo; cria a conta `INTERNO` a partir das variáveis de ambiente quando presentes.
4. `repositories/UserRepository.java` — `existsByCnpj`.
5. `dtos/UserDTO.java` — tipo obrigatório e `cnpj` opcional; `dtos/UserDetailDTO.java` expõe tipo e status.
6. `services/UserService.java` — grava o status inicial, recusa CNPJ ausente em PJ e CNPJ duplicado.
7. `exceptions/handler/GlobalExceptionHandler.java` — 409 para conflito de CNPJ.
8. `services/JwtService.java` — claims de tipo e papel.
9. `security/JwtAuthenticationFilter.java` — authorities a partir da claim de papel.
10. `config/SecurityConfig.java` — `/api/v1/backoffice/**` exige papel `INTERNO`.
11. `controllers/BackofficeController.java` — uma rota de verificação, só para C12 e C13 terem alvo.

Fatia única: os critérios de `seguranca` dependem do tipo e do papel existirem, então não fecham separados.

## Perguntas em aberto
nenhuma
