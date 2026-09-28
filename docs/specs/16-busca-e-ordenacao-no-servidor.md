# 16-busca-e-ordenacao-no-servidor

## Intenção
Quem procura imóvel busca no acervo inteiro e ordena por preço de verdade — hoje o filtro acontece no navegador, sobre o que coube na página.

## Depende de
15-anuncios-paginados

> **Aprovada no gate humano em 2026-09-28.** Todos os critérios aceitos.

## Critérios
- [x] **C1** `contrato-api` — `GET /api/v1/announcements` aceita o parâmetro `q`.
- [x] **C2** `dominio` — Busca por trecho do título devolve o anúncio correspondente.
- [x] **C3** `dominio` — Busca por nome da cidade devolve o anúncio correspondente.
- [x] **C4** `dominio` — Busca em caixa diferente da gravada devolve o anúncio.
- [x] **C5** `dominio` — Busca sem acento devolve anúncio cuja cidade tem acento.
- [x] **C6** `dominio` — Busca sem correspondência devolve página vazia com total zero.
- [x] **C7** `dominio` — Requisição sem `q` devolve o acervo inteiro paginado.
- [x] **C8** `dominio` — `q` em branco devolve o acervo inteiro paginado.
- [x] **C9** `dominio` — A busca respeita a paginação: com três correspondências e `size=2`, a página traz duas.
- [x] **C10** `contrato-api` — `sort=price,desc` traz o anúncio mais caro primeiro.
- [x] **C11** `contrato-api` — `sort=price,asc` traz o anúncio mais barato primeiro.
- [x] **C12** `dominio` — Sem `sort`, a ordem continua do mais recente para o mais antigo.
- [x] **C13** `seguranca` — Ordenação por campo fora do conjunto permitido é recusada com 400.
- [x] **C14** `persistencia` — O índice de busca existe após a migração.
- [x] **C15** `dominio` — A busca combina com a ordenação: `q` mais `sort=price,asc` devolve as correspondências da mais barata para a mais cara.

## Contrato técnico
- **O que é buscável sai do que a tela já fazia:** `AnnouncementList` filtrava por `title`, `city` e `state` concatenados. A busca no servidor cobre os mesmos três campos, nem mais nem menos.
- **Acento não pode importar.** "Sao Paulo", "são paulo" e "SÃO PAULO" precisam encontrar o mesmo imóvel: em português quem digita rápido não acentua.
- **A normalização vive numa coluna, não numa expressão de índice.** A redação anterior previa um invólucro `IMMUTABLE` sobre `unaccent`; ao implementar, a coluna se mostrou melhor por duas razões. A primeira é que o invólucro é pegadinha para quem mantiver o banco depois. A segunda é decisiva: com expressão de índice a consulta teria de ser nativa, e em consulta nativa o Spring monta o `ORDER BY` com o nome cru da propriedade — `createdAt` não existe no SQL, e C10 a C12 quebrariam. Com coluna, a consulta segue em JPQL e a ordenação do `Pageable` continua funcionando.
- A coluna é preenchida pela entidade em `@PrePersist`/`@PreUpdate`, então não envelhece por caminho JPA. Escrita direta em SQL, fora do JPA, deixaria a busca desatualizada.
- A normalização em Java usa `Normalizer` em NFD com remoção de diacríticos; a carga inicial da migração usa `unaccent` do Postgres. **As duas precisam concordar**, e concordam para os acentos do português.
- Índice **GIN com `pg_trgm`** sobre a coluna. A busca é `like '%termo%'`, com curinga à esquerda, e índice B-tree não serve para isso.
- **Ordenação tem lista de permissão:** `createdAt` e `price`. `Pageable` do Spring aceita qualquer nome de propriedade vindo da consulta, e ordenar por campo arbitrário é canal lateral — dá para inferir valor de coluna que ninguém devia ler. Fora da lista, 400.
- `q` ausente, vazio ou só espaço significa "sem filtro", não "nada encontrado".
- Migração `V3`, sob a disciplina da spec 12: extensão, função imutável e índice.

## Fora de escopo
- Filtro por tipo de imóvel, finalidade, faixa de preço, quartos ou área. A tela tem um seletor de tipo na home que hoje não filtra nada; ligá-lo é spec própria, com decisão sobre combinação de filtros.
- Busca por proximidade geográfica, CEP ou bairro.
- Relevância: o resultado sai na ordem pedida, não por quanto casa com o termo.
- Correção de digitação e busca fonética.
- Busca nas listagens da conta, nos favoritos e no histórico — esta spec é do acervo público.

## Plano
1. `src/main/resources/db/migration/V3__busca_de_anuncios.sql` — extensões, função imutável de normalização e índice GIN.
2. `repositories/AnnouncementRepository.java` — consulta de busca paginada.
3. `services/AnnouncementService.java` — `findAll` recebe o termo; termo em branco vira ausência de filtro.
4. `controllers/AnnouncementController.java` — parâmetro `q` e validação do campo de ordenação.
5. `src/test/java/com/example/demo/BuscaDeAnunciosSpecTest.java`.

Fatia única: a ordenação e a busca chegam pela mesma rota e se combinam em C15.

## Perguntas em aberto
nenhuma
