# Veredito — 13-falha-de-token-e-integridade

Base: estado da árvore após a spec 12, mais os arquivos novos de `.`.

> **Limitação de independência.** Produzido pela mesma sessão que escreveu o código, a pedido do humano. Leia sabendo que essa garantia não existe aqui.

Provado pela suíte e, para os critérios de borda, também contra a stack em Docker — foi lá que o defeito apareceu, e não em teste.

- **C1** — atendido. `GET /api/v1/announcements` com `Bearer nao-e-um-token` responde 200. Antes respondia 403 de corpo vazio: a exceção subia do filtro, o Tomcat despachava para `/error` e o `SecurityConfig` recusava esse despacho. Confirmado também em `/api/v1/plans`.

- **C2** — atendido. Mesmo caminho com token assinado e expirado. O teste constrói o token com `exp` no passado usando a mesma chave da aplicação, então a falha é de expiração e não de assinatura.

- **C3** — atendido. `GET /api/v1/announcements/me` com token malformado responde 403 — agora por não haver autenticação, não por exceção despachada.

- **C4** — atendido. Token válido com o último caractere trocado: estrutura intacta, assinatura quebrada. 403.

- **C5** — atendido. Token expirado em rota protegida: 403.

- **C6** — atendido. Token emitido para conta depois removida do banco: `loadUserByUsername` lança `UsernameNotFoundException`, capturada junto das falhas de token, e a requisição segue sem sessão.

- **C7** — atendido. Token válido de conta existente continua respondendo 200, na suíte e na stack.

- **C8** — atendido. `ListAppender` no logger raiz durante a requisição com token inválido: nenhum evento de nível `ERROR`. Na stack, a contagem de `MalformedJwtException`, `SignatureException` e `Servlet.service()` no log da API depois do redeploy é zero — antes havia uma entrada com rastro de pilha por tentativa.

- **C9** — atendido. `handleDataIntegrity` responde 409.

- **C10** — atendido. A mensagem devolvida é fixa e não contém o nome da restrição, o texto `duplicate key` nem a palavra `constraint`. A mensagem original vai para o log em nível `warn`, onde quem opera precisa dela.

## Observações que não são critério

- C9 e C10 se provam chamando o handler, não por requisição. Está declarado no contrato técnico: todo serviço que escreve nas restrições únicas consulta antes, então a violação só acontece em corrida entre duas requisições, e simular corrida produziria teste instável. O que se prova aqui é o mapeamento; que a corrida chegue até ele é inferência, não observação.
- O 403 de rota protegida continua com o corpo que o Spring Security produz — vazio. Quem consome não distingue "token expirado" de "sem permissão" pela resposta. Está no fora de escopo desta spec.
- O nível `debug` esconde token inválido por padrão. Um ataque de força bruta contra a rota de autenticação não deixa rastro no log da aplicação sem baixar o nível, e não há métrica que o revele.
- A expiração de uma hora continua igual. Esta spec faz o token expirado falhar direito; não faz a sessão durar mais nem se renovar.
