# 18-filtros-do-acervo

## Intenção
Quem procura imóvel recorta o acervo por cidade, faixa de preço, tipo e finalidade, em vez de só digitar um termo e rolar.

## Depende de
16-busca-e-ordenacao-no-servidor

## Critérios
- [ ] **C1** `dominio` — `city=Recife` devolve apenas anúncios cuja cidade é Recife.
- [ ] **C2** `dominio` — `city=joao pessoa` devolve o anúncio cuja cidade é "João Pessoa".
- [ ] **C3** `dominio` — `city=RECIFE` devolve o anúncio cuja cidade está gravada como "Recife".
- [ ] **C4** `dominio` — `minPrice` devolve apenas anúncios com preço maior ou igual ao valor.
- [ ] **C5** `dominio` — `maxPrice` devolve apenas anúncios com preço menor ou igual ao valor.
- [ ] **C6** `dominio` — `minPrice` e `maxPrice` juntos devolvem apenas anúncios dentro da faixa fechada.
- [ ] **C7** `dominio` — `type=APARTMENT` devolve apenas anúncios de apartamento.
- [ ] **C8** `dominio` — `type=HOUSE&type=LAND` devolve casas e terrenos, e nenhum apartamento.
- [ ] **C9** `dominio` — `reason=RENT` devolve apenas anúncios de aluguel.
- [ ] **C10** `dominio` — `q` combinado com `city` devolve apenas anúncios que satisfazem os dois.
- [ ] **C11** `dominio` — `type` combinado com `maxPrice` devolve apenas anúncios que satisfazem os dois.
- [ ] **C12** `dominio` — Requisição sem nenhum filtro devolve o acervo inteiro paginado.
- [ ] **C13** `dominio` — Combinação sem correspondência devolve página vazia com total zero.
- [ ] **C14** `seguranca` — `type` com valor fora do enum é recusado com 400.
- [ ] **C15** `contrato-api` — `type=APARTMENT` com `sort=price,asc` traz o apartamento mais barato primeiro.

## Contrato técnico
- **Os filtros se combinam por interseção, nunca por união.** Cada parâmetro presente estreita o resultado; parâmetro ausente, vazio ou só espaço não restringe nada. É a mesma regra que a spec 16 fixou para `q`, e vale para todos.
- **Consulta única em JPQL, com guarda de nulo por filtro** (`:param is null or ...`). Duas razões, e a segunda é decisiva: ramificar entre consultas por combinação de filtros dá 2⁵ caminhos, e consulta nativa quebraria a ordenação — em consulta nativa o Spring monta o `ORDER BY` com o nome cru da propriedade, `createdAt` não existe no SQL, e C15 mais os critérios C10 a C12 da spec 16 cairiam junto.
- **Cidade é comparada por coluna normalizada, não pela coluna crua.** A spec 16 já estabeleceu que acento não pode importar em português. `searchText` não serve aqui: ela concatena título, cidade e estado, e filtrar cidade contra ela faria um anúncio chamado "Casa em Recife" na Paraíba aparecer como se fosse de Recife. Coluna própria `city_search`, preenchida em `@PrePersist`/`@PreUpdate` pela entidade, mesma normalização de `searchText`.
- **Cidade casa por igualdade, não por trecho.** `city=Rio` não pode trazer "Rio de Janeiro" e "Rio Branco" juntos. Termo livre é papel do `q`.
- `type` é repetível e vale como "qualquer um destes"; um valor só é o caso degenerado. `reason` aceita um valor só — venda e aluguel juntos é a ausência do filtro.
- **Faixa invertida (`minPrice` maior que `maxPrice`) devolve página vazia, não 400.** É consulta sem resultado, não requisição malformada; impedir a inversão é trabalho da tela.
- Migração `V4`, sob a disciplina da spec 12: coluna nova e carga inicial com `unaccent`.
- Sem índice novo. O acervo não tem volume que justifique, e índice sem medição é adivinhação — se a consulta pesar, isso vira spec de performance com número na mão.

## Fora de escopo
- **Bairro.** O campo não existe no domínio: não há coluna, não há no schema Zod e não há no formulário de anúncio. Criar o dado é demanda própria, e ela precisa decidir o que fazer com os anúncios já publicados.
- Filtro por quartos, banheiros e área — os campos existem, mas cada um amplia o contrato e nenhum foi pedido.
- Busca por proximidade, raio ou CEP.
- Contagem por faceta ("142 apartamentos, 38 casas"): exige uma agregação por consulta e é decisão de produto sobre o que mostrar.
- Filtro salvo, alerta por busca e histórico de filtros.
- **A tela.** Os controles em `/imoveis` são a spec seguinte, no repositório do front.

## Plano
1. `src/main/resources/db/migration/V4__cidade_normalizada.sql` — coluna `city_search` e carga inicial com `unaccent`.
2. `model/Announcement.java` — `citySearch` preenchida no mesmo método que já mantém `searchText`.
3. `repositories/AnnouncementRepository.java` — consulta única com guarda de nulo para termo, cidade, faixa de preço, tipos e finalidade.
4. `services/AnnouncementService.java` — `findAll` recebe os filtros; branco vira ausência de filtro, como já faz com o termo.
5. `controllers/AnnouncementController.java` — parâmetros `city`, `minPrice`, `maxPrice`, `type` e `reason`.
6. `src/test/java/com/example/demo/FiltrosDoAcervoSpecTest.java`.

Fatia única: os filtros chegam pela mesma rota e a interseção entre eles é o que C10, C11 e C15 provam.

## Perguntas em aberto
nenhuma
