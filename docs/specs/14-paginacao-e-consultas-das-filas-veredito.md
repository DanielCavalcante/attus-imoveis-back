# Veredito — 14-paginacao-e-consultas-das-filas

Base: estado da árvore após a spec 13, mais os arquivos novos de `.`.

> **Limitação de independência.** Produzido pela mesma sessão que escreveu o código, a pedido do humano. Leia sabendo que essa garantia não existe aqui.

- **C1 a C4** — atendidos. As duas filas aceitam `page` e `size`, a resposta traz `totalElements`, e com três empresas e `size=2` a primeira página traz duas e a segunda traz uma.

- **C5** — atendido. A fila de empresas ordena por `id` crescente, que é a ordem de chegada; o teste guarda os ids na ordem em que entraram e confere o primeiro e o terceiro.

- **C6** — atendido. A fila de apoio pagina do mesmo jeito.

- **C7** — atendido. Ordena por `createdAt` crescente. O teste espaça os pedidos em 15 ms para que a ordem não dependa de resolução de relógio.

- **C8** — atendido, com o número medido e não estimado. Antes: seis consultas para cinco pedidos. Depois: dentro do teto de três. O caminho até aqui está nas observações.

- **C9** — atendido. `findAllByAccountTypeIsNull` substitui `findAll().stream().filter(...)`.

- **C10** — atendido. Com cinco contas tipadas e uma sem tipo, a consulta traz uma linha.

- **C11** — atendido. Carregar uma conta pelo e-mail dispara no máximo uma consulta. Antes eram duas: a conta e a empresa dela.

- **C12** — atendido. Removida a conta, a empresa some — agora por `ON DELETE CASCADE` no banco, não por cascade do JPA. Provado no teste e na migração aplicada ao banco de desenvolvimento, onde a restrição passou a ter `confdeltype = c`.

## Observações que não são critério

- **O alvo original não era o defeito principal.** A spec nasceu para matar um N+1 na fila de apoio. O `join fetch` matou esse — a fila vem em uma consulta —, mas a medição continuou em seis. As cinco restantes eram `select from companies where user_id=?`, uma por conta carregada, causadas pelo `@OneToOne(mappedBy)` que a spec 02 acrescentou a `User` só para cascatear remoção. O lado inverso de um `@OneToOne` não é lazy sem bytecode enhancement. **Isso custava uma consulta extra em toda requisição autenticada**, porque `loadUserByUsername` carrega um `User` — bem além da fila que a spec pretendia consertar.
- `@EntityGraph` foi a primeira tentativa e não resolveu: a medição ficou nas mesmas seis consultas. O `join fetch` explícito resolveu. A diferença não foi investigada a fundo.
- **O painel do frontend está quebrado neste momento.** As duas filas devolvem `Page` e o painel espera array. Está declarado no contrato técnico e a spec seguinte, no outro repositório, precisa entrar antes de alguém usar o painel.
- Os testes das specs 05 e 09 tiveram os caminhos JSON ajustados de `$[0]` para `$.content[0]`. Os critérios daquelas specs não mudaram — o que mudou foi a forma da resposta, e a observação seguiu a mesma.
- Paginar `GET /api/v1/announcements`, favoritos e histórico **continua aberto**, e é a observação de volume mais consequente da homologação: é o caminho público e mais quente.
- Não há índice em `analysis_status` nem em `status`. Com volume, a fila varre a tabela.
