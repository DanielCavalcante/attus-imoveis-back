# 15-anuncios-paginados

## Intenção
A listagem pública de imóveis, a da conta, os favoritos e o histórico param de carregar a tabela inteira a cada requisição.

## Depende de
14-paginacao-e-consultas-das-filas

## Critérios
- [ ] **C1** `contrato-api` — `GET /api/v1/announcements` aceita `page` e `size`.
- [ ] **C2** `contrato-api` — A resposta da listagem pública traz o total de elementos.
- [ ] **C3** `dominio` — Com três anúncios e `size=2`, a primeira página traz dois.
- [ ] **C4** `dominio` — Com três anúncios e `size=2`, a segunda página traz o restante.
- [ ] **C5** `dominio` — A listagem pública vem do mais recente para o mais antigo.
- [ ] **C6** `contrato-api` — `GET /api/v1/announcements` sem `page` nem `size` devolve página de tamanho 12.
- [ ] **C7** `performance` — `size=1000` é reduzido ao teto de 50.
- [ ] **C8** `performance` — A listagem pública com dez anúncios e `size=5` devolve cinco elementos.
- [ ] **C9** `seguranca` — A listagem pública paginada continua acessível sem autenticação.
- [ ] **C10** `contrato-api` — `GET /api/v1/announcements/me` pagina.
- [ ] **C11** `contrato-api` — `GET /api/v1/announcements/me/favoritos` pagina.
- [ ] **C12** `contrato-api` — `GET /api/v1/announcements/me/historico` pagina.
- [ ] **C13** `dominio` — O histórico paginado mantém a ordem do mais recente para o mais antigo.
- [ ] **C14** `dominio` — Favorito de outra conta continua fora da página desta.

## Contrato técnico
- **O teto de página é a parte que importa.** Paginar sem limitar `size` não protege nada: `?size=1000000` volta a carregar a tabela. `spring.data.web.pageable.max-page-size=50` fecha a porta, e o Spring reduz silenciosamente em vez de recusar — quem pede demais recebe o teto, não um erro.
- Tamanho padrão da listagem pública: **12**, que é múltiplo de 2, 3 e 4 e fecha as grades de uma, duas, três e quatro colunas do frontend sem linha órfã. As demais seguem em 20.
- A listagem pública ordena por `createdAt` decrescente. Hoje `findAll()` devolve na ordem que o banco entregar, o que na prática é ordem de inserção e ninguém garantiu.
- `AnnouncementListDTO` lê só campos do próprio anúncio, sem associação: a listagem não tem N+1 para resolver, e a medição confirma isso em vez de supor.
- A resposta das quatro rotas passa a ser `Page`. **Isto quebra o contrato com a home, a listagem, o dashboard e as telas de conta do frontend** — todas elas. A spec seguinte, no outro repositório, é obrigatória antes de qualquer uso.

## Fora de escopo
- Filtro e busca na listagem pública: a tela tem um campo de busca que hoje filtra no cliente, sobre a lista inteira. Quando a lista deixar de ser inteira, a busca precisa ir para o servidor — e isso é spec própria, com índice e decisão sobre o que é buscável.
- Rolagem infinita ou cursor: `offset` basta no volume previsto.
- Índice em `created_at`.
- Ordenação escolhida por quem consome (maior preço, menor preço): o `<select>` da tela também ordena no cliente hoje, e cai no mesmo problema da busca.

## Plano
1. `repositories/AnnouncementRepository.java` — `findByUserEmail` com `Pageable`.
2. `repositories/FavoriteRepository.java` e `repositories/AnnouncementViewRepository.java` — consultas paginadas.
3. `services/AnnouncementService.java` — `findAll` e `findByUser` com `Pageable`.
4. `services/UserLibraryService.java` — `favorites` e `history` com `Pageable`.
5. `controllers/AnnouncementController.java` — `Pageable` nas quatro rotas.
6. `src/main/resources/application.properties` — teto e tamanho padrão de página.
7. `src/test/java/com/example/demo/AnunciosPaginadosSpecTest.java`.

Fatia única: as quatro rotas compartilham o teto e a mesma forma de resposta.

## Perguntas em aberto
nenhuma
