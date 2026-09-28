# Veredito — 16-busca-e-ordenacao-no-servidor

Base: estado da árvore após a spec 15, mais os arquivos novos de `.`.

> **Limitação de independência.** Produzido pela mesma sessão que escreveu o código, a pedido do humano. Leia sabendo que essa garantia não existe aqui.

- **C1** — atendido. `GET /api/v1/announcements` aceita `q`.

- **C2** — atendido. Busca por "cobertura" devolve só o anúncio cujo título a contém, com dois anúncios gravados.

- **C3** — atendido. Busca por "curitiba" separa por cidade.

- **C4** — atendido. "COBERTURA duplex" encontra "Cobertura Duplex": a coluna e o termo são normalizados para minúscula.

- **C5** — atendido **nos dois sentidos**. "sao paulo" encontra cidade gravada como "São Paulo", e "São Paulo" também. Um sentido só passaria com normalização pela metade.

- **C6** — atendido. Termo sem correspondência devolve `totalElements = 0` e `content` vazio, não erro.

- **C7 e C8** — atendidos. Sem `q` e com `q` em branco o acervo inteiro volta paginado: ausência de termo é ausência de filtro, não filtro que não casa com nada.

- **C9** — atendido. Três correspondências com `size=2` devolvem dois elementos e total três.

- **C10 e C11** — atendidos. `sort=price,desc` e `sort=price,asc` respeitam a direção pedida.

- **C12** — atendido. Sem `sort`, o mais recente vem primeiro.

- **C13** — atendido. `sort=user.password,asc` e `sort=searchText,asc` respondem 400. O primeiro é o caso que motiva o critério: ordenar por coluna que ninguém devia ler permite inferir valor comparando a ordem entre requisições.

- **C14** — atendido. `idx_announcements_busca` existe em `pg_indexes`.

- **C15** — atendido. `q=praia` com `sort=price,asc` devolve as duas correspondências da mais barata para a mais cara, e exclui o terceiro anúncio, que é mais barato que as duas mas não casa com o termo. A combinação importa: filtro certo com ordem errada, ou o inverso, passaria num critério isolado.

Além da suíte, a migração `V3` foi aplicada ao banco de desenvolvimento, que tem anúncios gravados desde antes da coluna existir. A carga preencheu `search_text` para todos eles — "Sao Paulo" virou `... sao paulo sp` — e a busca por "sao paulo" na stack encontra o anúncio.

## Observações que não são critério

- **A normalização existe em dois lugares que precisam concordar:** `Normalizer` em Java, na entidade, e `unaccent` do Postgres, na carga da migração. Concordam para os acentos do português, que é o caso deste produto. Divergiriam para alfabetos que o `unaccent` trata e o NFD não, e nada no código detecta essa divergência.
- `search_text` é mantida em `@PrePersist`/`@PreUpdate`, então não envelhece por caminho JPA. **Escrita direta em SQL deixa a busca desatualizada** sem aviso — inclusive uma futura migração que altere título ou cidade.
- A busca é `like '%termo%'` sobre uma coluna concatenada: não há relevância, e um termo que case com o título vale o mesmo que um que case com a UF. Declarado no fora de escopo.
- A lista de permissão de ordenação vive no controller de anúncios. As outras rotas paginadas — filas do backoffice, favoritos, histórico — continuam aceitando qualquer campo em `sort`.
