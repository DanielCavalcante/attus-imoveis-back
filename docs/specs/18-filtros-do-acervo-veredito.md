# 18-filtros-do-acervo — veredito

Medido contra `51c3268..HEAD`, escopo `.`, com `FiltrosDoAcervoSpecTest` (15/15)
mais `BuscaDeAnunciosSpecTest`, `AnunciosPaginadosSpecTest` e
`QuotaMensalDaPfSpecTest` (42 no total, todos verdes).

- **C1** — atendido. `city=Recife` com dois anúncios semeados em cidades diferentes devolve `totalElements=1` e `content[0].city=Recife`. A comparação é por igualdade sobre `city_search`, não por trecho.
- **C2** — atendido. `city=joao pessoa` encontra o anúncio gravado como "João Pessoa". A coluna `city_search` é preenchida em `@PrePersist`/`@PreUpdate` pela mesma normalização NFD que já alimenta `search_text`.
- **C3** — atendido. `city=RECIFE` encontra o anúncio gravado como "Recife": a normalização baixa a caixa antes da comparação.
- **C4** — atendido. `minPrice=500000` sobre acervo de 100.000 e 900.000 devolve só o de 900.000.
- **C5** — atendido. `maxPrice=500000` sobre o mesmo acervo devolve só o de 100.000.
- **C6** — atendido. Faixa 400.000–600.000 sobre 100.000, 500.000 e 900.000 devolve só o do meio. A faixa é fechada dos dois lados (`>=` e `<=`).
- **C7** — atendido. `type=APARTMENT` sobre um apartamento e uma casa devolve só o apartamento.
- **C8** — atendido. `type=HOUSE&type=LAND` devolve dois e o JSONPath `$.content[?(@.propertyType == 'APARTMENT')]` vem vazio — a união é dos tipos pedidos e não inclui o terceiro.
- **C9** — atendido. `reason=RENT` devolve só o anúncio de aluguel.
- **C10** — atendido. `q=cobertura` mais `city=Recife` sobre duas coberturas em cidades diferentes e uma kitnet em Recife devolve exatamente uma: a interseção, não a união.
- **C11** — atendido. `type=HOUSE` mais `maxPrice=500000` sobre casa barata, casa cara e apartamento barato devolve só a casa barata.
- **C12** — atendido. Sem nenhum parâmetro, os três anúncios semeados voltam. Texto ausente vira string vazia e o ramo `:termo = ''` curto-circuita; tipo ausente vira o enum inteiro.
- **C13** — atendido. `city=Recife` mais `maxPrice=100000` sobre uma casa de 900.000 devolve `totalElements=0` e `content` vazio — página vazia, não erro.
- **C14** — atendido. `type=MANSAO` devolve 400. A recusa vem do binding do Spring sobre o enum, antes de qualquer consulta.
- **C15** — atendido. `type=APARTMENT` com `sort=price,asc` devolve dois e traz "Apto barato" primeiro, com a casa de 50.000 fora do resultado. Confirma que o filtro e a ordenação convivem, e que a consulta em JPQL preserva o `ORDER BY` do `Pageable` por nome de propriedade.

## Observações que não são critério

- A guarda de nulo sobre os dois filtros de texto **não funcionou** e teve de ser trocada por sentinela vazia. Com parâmetro nulo o Postgres não infere o tipo dentro do `LIKE` e recusa a consulta inteira com `operator does not exist: text ~~ bytea`. Foi falha real na primeira execução: 13 dos 15 testes quebraram. Nulo segue valendo nos filtros numéricos e no enum, onde o Hibernate tipa o parâmetro pela própria propriedade comparada.
- `Announcement.normalizar` passou a ser `public static` para servir também ao `AnnouncementFilter`, mantendo uma fonte só de normalização entre a entidade e a consulta.
- A suíte da spec 16 continua verde depois da troca de `buscar` por `filtrar`, incluindo os critérios de ordenação que dependem de a consulta seguir em JPQL.
