# Veredito — 01-contas-tipadas

> **Reaberto em 2026-09-28.** O gate emendou C5 e C6: a conta passou a nascer `ATIVO`
> em vez de `PENDENTE_EMAIL`, porque o serviço de e-mail foi adiado. As duas linhas
> abaixo foram reavaliadas contra o código novo; as demais não foram tocadas pela emenda.

Base: `1e03b12..HEAD` mais os não rastreados de `.` (o trabalho da spec está na árvore, não commitado).

> **Limitação de independência.** Este parecer foi produzido pela mesma sessão que escreveu o código, a pedido do humano. O passo 2 do `verificar` pede contexto limpo justamente porque quem escreve o código escreve os testes, e suíte verde é autoatestada. Leia-o sabendo que ele não tem essa garantia.

- **C1** — atendido. `UserDTO.accountType` recebeu `@NotNull`, o controller já anotava `@Valid`, e o handler novo de `MethodArgumentNotValidException` monta a mensagem como `"<campo>: <mensagem>"`, então o corpo da resposta contém a string `accountType`.

- **C2** — atendido por dois caminhos distintos. Valor desconhecido (`"SINDICATO"`) falha na desserialização do enum e cai em `HttpMessageNotReadableException`, que passou a responder 400 em vez de 500. Valor `INTERNO` desserializa e é barrado por `UserService.validateAccountType`, que só aceita `PF` e `PJ` — o conjunto aceito por esta porta.

- **C3** — atendido. `UserService.create` faz `entity.setAccountType(dto.accountType())` e o campo é `@Enumerated(EnumType.STRING)`.

- **C4** — atendido. Mesma linha de C3; nada no caminho de PJ desvia a atribuição.

- **C5** — atendido (reavaliado). `entity.setAccountStatus(AccountStatus.ATIVO)` é incondicional, sem ramo por tipo. `PENDENTE_EMAIL` permanece no enum, sem nenhum caminho de código que o produza.

- **C6** — atendido (reavaliado). A mesma linha incondicional cobre PJ.

- **C7** — atendido. `validateCnpj` lança `BusinessException` quando o tipo é PJ e o CNPJ é nulo ou em branco, e `BusinessException` já respondia 400.

- **C8** — atendido. `validateCnpj` consulta `existsByCnpj` e lança `ConflictException`, cujo handler novo responde 409. Limite observado: a unicidade também existe como constraint no banco, e o caminho de corrida entre duas requisições simultâneas cai em `DataIntegrityViolationException`, que não tem handler — mas isso está fora da observação que o critério enuncia.

- **C9** — atendido. `AccountBootstrap.backfillExistingAccounts` seleciona as contas com `accountType == null` (o que caracteriza conta anterior a esta spec, já que toda conta nova recebe tipo na criação) e grava `ATIVO`. O teste insere uma conta sem tipo, chama a carga e relê do banco.

- **C10** — atendido. `JwtService.generateToken` acrescenta `claims.put("accountType", ...)`, e o teste decodifica o payload do token em vez de confiar num getter.

- **C11** — atendido. `claims.put("role", ...)` na mesma linha de emissão.

- **C12** — atendido. `SecurityConfig` fecha `/api/v1/backoffice/**` com `hasRole("INTERNO")`, e `UserRole.authority()` produz `ROLE_INTERNO`, que é a forma que `hasRole` procura. Token de papel `COMUM` recebe 403 na rota `/ping`.

- **C13** — atendido. Mesmo caminho com papel `INTERNO` chega ao controller e responde 204, que não é 403.

> **CORREÇÃO na homologação do ciclo 01, 2026-09-28.** O contrato técnico da spec 01 diz "não existe credencial interna padrão no repositório", e isto era verdade quando este veredito foi escrito. A spec 08 introduziu `interno@attus.dev` / `InternoDev123` como valores default literais em `docker-compose.yml:40-42`, versionado. A afirmação deixou de valer e a contradição não foi reconciliada em nenhuma spec.

- **C14** — atendido por duas barreiras independentes. `UserDTO` não tem campo `role`, então nada no corpo é desserializado para lá; e `UserService.create` grava `UserRole.COMUM` incondicionalmente. O teste envia `"role": "INTERNO"` no corpo e relê a conta gravada.

- **C15** — atendido. As regras de `permitAll` para `GET /api/v1/announcements` não foram tocadas; a regra nova do backoffice entra antes de `anyRequest()` e não intercepta esse caminho.

## Observações que não são critério

- `JwtService.generateToken` chama `.name()` direto em `getAccountType()` e `getRole()`. Conta com esses campos nulos faz o login estourar `NullPointerException`. Na prática a carga roda no boot e o cadastro sempre atribui os dois, então não há origem conhecida de nulo — mas o código não se defende disso.
- `validateCnpj` retorna cedo quando o tipo não é PJ, então um cadastro PF que envie `cnpj` como string vazia grava `""`. Dois cadastros assim colidem na constraint de unicidade.
