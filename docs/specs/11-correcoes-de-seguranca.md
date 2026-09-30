# 11-correcoes-de-seguranca

## Intenção
Fecha duas aberturas que a homologação do ciclo 01 expôs: uma rota autenticada servida sem sessão, e autorização que ignora o estado atual da conta.

## Depende de
01-contas-tipadas

> **Aprovada no gate humano em 2026-09-28.** Todos os critérios aceitos.

## Critérios
- [x] **C1** `seguranca` — `GET /api/v1/announcements/me` sem cabeçalho de autorização responde 403.
- [x] **C2** `contrato-api` — `GET /api/v1/announcements/me` com sessão continua respondendo 200.
- [x] **C3** `contrato-api` — `GET /api/v1/announcements` sem autenticação continua respondendo 200.
- [x] **C4** `contrato-api` — `GET /api/v1/announcements/{id}` sem autenticação continua respondendo 200.
- [x] **C5** `contrato-api` — `GET /api/v1/announcements/me/quota` sem autenticação continua respondendo 403.
- [x] **C6** `seguranca` — Conta rebaixada para papel `COMUM` no banco recebe 403 no backoffice usando token emitido enquanto era `INTERNO`.
- [x] **C7** `seguranca` — Conta promovida a papel `INTERNO` no banco acessa o backoffice usando token emitido enquanto era `COMUM`.
- [x] **C8** `seguranca` — Pedido de apoio em anúncio alheio continua respondendo 403.

## Contrato técnico
- **Emenda ao contrato técnico da spec 01.** Ela dizia que as authorities saem da claim "sem consulta extra ao banco". A justificativa era falsa: `JwtAuthenticationFilter` já chama `loadUserByUsername` em toda requisição autenticada, então ler o papel da claim não evitava consulta alguma — apenas abria mão de revogar acesso antes de o token expirar. As authorities passam a vir do `UserDetails` carregado, que é o estado atual da conta.
- A claim `role` **continua no token**, porque o frontend a usa para decidir o que desenhar. Ela deixa de ser fonte de autorização no servidor. Quem decide é o banco; a claim é dica de interface.
- O `permitAll` de `/api/v1/announcements/*` casava com `/me`. A regra passa a nomear explicitamente as rotas `/me` como autenticadas **antes** da regra curinga: em `authorizeHttpRequests` o primeiro casamento vence, então a ordem é a correção.
- `JwtService.extractRole` permanece: é o que prova, no teste da spec 01 C11, que a claim é emitida.

## Fora de escopo
- Revogação imediata por invalidação de token (lista de bloqueio, tempo de vida menor, refresh token): a consulta ao banco por requisição já resolve o caso que motivou esta spec.
- Conta apagada do banco com token válido — `loadUserByUsername` lança `UsernameNotFoundException`, sem handler, e responde 500. É outra abertura, de outra spec.
- `DataIntegrityViolationException` sem handler nas quatro constraints únicas novas.
- Migração versionada de schema e paginação das filas.

## Plano
1. `config/SecurityConfig.java` — rotas `/api/v1/announcements/me` e `/me/**` autenticadas antes do curinga.
2. `security/JwtAuthenticationFilter.java` — authorities do `UserDetails` carregado, não da claim.

Fatia única: os dois pontos são de `seguranca` e se provam na mesma suíte.

## Perguntas em aberto
nenhuma
