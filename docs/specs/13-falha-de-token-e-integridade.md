# 13-falha-de-token-e-integridade

## Intenção
Token ruim deixa de derrubar rota pública e de encher o log com rastro de pilha; violação de restrição única deixa de virar erro sem significado.

## Depende de
11-correcoes-de-seguranca

> **Aprovada no gate humano em 2026-09-29.** Todos os critérios aceitos.

## Critérios
- [x] **C1** `contrato-api` — `GET /api/v1/announcements` com token malformado no cabeçalho responde 200.
- [x] **C2** `contrato-api` — `GET /api/v1/announcements` com token expirado no cabeçalho responde 200.
- [x] **C3** `seguranca` — `GET /api/v1/announcements/me` com token malformado responde 403.
- [x] **C4** `seguranca` — `GET /api/v1/announcements/me` com assinatura inválida responde 403.
- [x] **C5** `seguranca` — `GET /api/v1/announcements/me` com token expirado responde 403.
- [x] **C6** `seguranca` — `GET /api/v1/announcements/me` com token de conta removida do banco responde 403.
- [x] **C7** `seguranca` — Token válido de conta existente continua autenticando.
- [x] **C8** `observabilidade` — Requisição com token inválido não produz registro de nível `ERROR`.
- [x] **C9** `contrato-api` — `DataIntegrityViolationException` responde 409.
- [x] **C10** `seguranca` — A resposta 409 de integridade não contém o texto da exceção do banco.

## Contrato técnico
- **O filtro JWT não deixa exceção escapar.** `MalformedJwtException`, `ExpiredJwtException`, `SignatureException` e `UsernameNotFoundException` viram "esta requisição não tem sessão": o filtro segue a cadeia sem autenticar. Quem decide o que fazer com isso é o `SecurityConfig` — rota pública passa, rota protegida responde 403.
- Hoje a exceção sobe do filtro, o Tomcat despacha para `/error`, o cliente recebe 403 de corpo vazio e o servidor registra rastro de pilha em nível `ERROR`. O efeito colateral é que **rota pública deixa de responder** quando há cabeçalho ruim, o que atinge qualquer visitante com token velho guardado.
- O registro passa a ser `debug`, não `warn` nem `error`: token inválido é situação normal e de origem externa. Em `error` qualquer pessoa enche o disco de quem opera.
- `DataIntegrityViolationException` responde **409** com mensagem fixa. A mensagem do banco nomeia tabela e restrição, e isso não vai para quem chama.
- A violação de integridade **não é alcançável de forma determinística por endpoint**: todo serviço que escreve nas quatro restrições únicas do ciclo consulta antes. Ela existe para a corrida entre duas requisições. Por isso C9 e C10 se provam no handler, e o teste declara isso em vez de simular uma corrida que não se reproduz.

## Fora de escopo
- Refresh token e tempo de vida do token: a spec 11 já declarou isso fora, e nada aqui muda a expiração de uma hora.
- Corpo de erro padronizado para todas as respostas do Spring Security: `403` de rota protegida continua com o corpo que o framework produz.
- Métrica ou alarme sobre volume de token inválido.
- As demais pendências da homologação — paginação, N+1, estado de origem, reativação de plano.

## Plano
1. `security/JwtAuthenticationFilter.java` — captura de falha de token e de conta ausente.
2. `exceptions/handler/GlobalExceptionHandler.java` — `DataIntegrityViolationException` para 409.
3. `src/test/java/com/example/demo/FalhaDeTokenSpecTest.java`.

Fatia única: os dois pontos são de robustez de borda e se provam na mesma suíte.

## Perguntas em aberto
nenhuma
