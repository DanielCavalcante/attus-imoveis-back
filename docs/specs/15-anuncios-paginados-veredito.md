# Veredito — 15-anuncios-paginados

Base: estado da árvore após a spec 14, mais os arquivos novos de `.`.

> **Limitação de independência.** Produzido pela mesma sessão que escreveu o código, a pedido do humano. Leia sabendo que essa garantia não existe aqui.

- **C1 a C4** — atendidos. A listagem pública aceita `page` e `size`, traz `totalElements`, e com três anúncios e `size=2` a primeira página traz dois e a segunda traz um.

- **C5** — atendido. Ordena por `createdAt` decrescente. O teste guarda os ids na ordem de criação e confere que o primeiro da página é o último criado. Antes, `findAll()` devolvia na ordem que o banco entregasse — na prática a de inserção, mas sem ninguém garantir.

- **C6** — atendido. Sem parâmetros, a página tem tamanho 12.

- **C7** — atendido, e é o critério que dá sentido aos outros. `size=1000` volta com `size=50`. Sem o teto, paginar não protegeria nada: bastaria `?size=1000000` para carregar a tabela de novo. O Spring reduz em silêncio, então quem pede demais recebe o máximo em vez de um erro.

- **C8** — atendido. Dez anúncios com `size=5` devolvem cinco elementos e `totalElements=10`.

- **C9** — atendido. A listagem paginada segue pública.

- **C10, C11, C12** — atendidos. A listagem da conta, os favoritos e o histórico paginam com a mesma forma de resposta.

- **C13** — atendido. O histórico paginado mantém o mais recente primeiro. O teste espaça as visitas em 15 ms para que a ordem não dependa da resolução do relógio.

- **C14** — atendido. Favorito de outra conta continua fora: a consulta filtra por e-mail e a contagem vem zerada.

## Observações que não são critério

- **A busca e a ordenação da tela deixaram de funcionar, e isso é consequência direta desta spec.** `AnnouncementList` filtra e ordena no cliente, sobre a lista inteira que a API devolvia. Com página de 12, ela passa a filtrar 12 itens e a chamar isso de resultado. Está declarado no fora de escopo, mas o efeito é imediato: a partir do momento em que o frontend consumir a página, a busca mente.
- **O frontend está quebrado em mais lugares agora**: home, listagem, dashboard e as telas de conta esperam array. Junto com o painel do backoffice da spec 14, são seis telas.
- Os testes das specs 01 e 10 tiveram os caminhos JSON ajustados de `$[0]` para `$.content[0]`. Os critérios daquelas specs não mudaram.
- Não há índice em `created_at`, e a listagem pública passou a ordenar por ele. Sem volume medido não se criou, mas a ordenação é de toda requisição da rota mais quente.
- `AnnouncementListDTO` lê só campos do próprio anúncio, então a listagem não tem N+1 — verificado ao ler o DTO, não medido com contador.
