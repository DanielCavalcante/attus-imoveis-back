# CICLO 02 — homologação

Medido em 2026-09-29, na branch `feat/docker-e-polish` dos dois repositórios.

**Ref base do ciclo:** back `51c3268`, front `3a01505`.
**HEAD medido:** back `8a70b32`, front `a792865`.

Seis specs, todas com veredito, nenhuma em quarentena:

| repo | spec |
|---|---|
| back | `18-filtros-do-acervo` · `19-bairro-derivado-do-cep` · `22-anuncio-ativo-e-quota-por-ativos` |
| front | `20-busca-da-home` · `21-sugestoes-de-cidade-e-bairro` · `23-pagina-de-gerenciamento-dos-anuncios` |

O back tem um commit a mais que as três specs dele: `8a70b32`
(`fix: PATCH liberado no CORS e active exposto na listagem da conta`), nascido
durante a verificação da spec 23 — do outro repositório.

---

## O que mudou

- **Acervo recortável (spec 18).** `GET /announcements` passou a aceitar `city`,
  `minPrice`, `maxPrice`, `type` (repetível) e `reason`, numa consulta JPQL única
  com guarda por filtro. Cidade compara por coluna normalizada própria
  (`city_search`, migração `V4`), não por `search_text`.
- **Bairro sem cadastro (spec 19).** `V5` criou `neighborhood` e
  `neighborhood_search`; o valor sai do CEP via `ConsultaDeCep`/`ViaCepConsulta`
  no servidor, com 2s de tempo limite e falha silenciosa. Rotas novas
  `/announcements/cidades` e `/announcements/bairros?city=` devolvem `distinct`
  sobre o acervo. `CargaDeBairros` preenche os antigos, fora da migração.
- **Anúncio ativo e quota por ativos (spec 22).** `V6` trouxe `active NOT NULL
  DEFAULT TRUE`. Entraram `PATCH /{id}/inativar`, `PATCH /{id}/reativar` e
  `DELETE /{id}`, as três com dono verificado (404 para quem não é dono). A chave
  `attus.quota.pf.mensal` virou `attus.quota.pf.ativos` e a contagem saiu do ramo
  da PF: contar é de todo mundo, o teto é que varia. O C6 da spec 06 foi marcado
  como substituído no próprio arquivo.
- **Home que pergunta (spec 20).** O campo de termo livre saiu; entraram cidade,
  bairro e tipo, e `/imoveis` passou a ler os três da URL.
- **Sugestões (spec 21).** `CampoComSugestoes`, combobox ARIA próprio, sobre as
  rotas de cidades e bairros. O harness de teste cresceu: `jsdom`,
  `@testing-library/*`, `include` aceitando `.tsx`.
- **Gerenciamento dos anúncios (spec 23).** `/portal/anuncios` dentro de
  `PrivateRoute`, com estado, saldo de quota e as três ações. A grade saiu do
  dashboard. No caminho, `app/services/api.ts` aprendeu a lidar com 204 e a não
  mascarar o erro real, e o CORS do back ganhou `PATCH`.

---

## O que está vermelho

Nada. Nenhuma suíte reprovou.

| suíte | executa | passa | falha | pula |
|---|---|---|---|---|
| back — `./mvnw test` (18 classes, Testcontainers) | 218 | 217 | 0 | 1 |
| front — `npm test` (Vitest, 7 arquivos) | 46 | 46 | 0 | 0 |
| front — `npm run build` (portão de tipos) | 13 rotas | compila, 0 erro de tipo | 0 | — |
| front — `npm run lint` | — | — | 4 erros, 6 avisos | — |

O único `Skipped` do back é `CrudApiApplicationTests`, que já nasceu pulado.

### Por classe, no back

| classe | run | fail |
|---|---|---|
| AnaliseDeConfiabilidadeSpecTest | 15 | 0 |
| AnunciosPaginadosSpecTest | 14 | 0 |
| ApoioDeVendaSpecTest | 15 | 0 |
| **BairroDoAnuncioSpecTest** (spec 19) | 14 | 0 |
| BuscaDeAnunciosSpecTest | 15 | 0 |
| ContasTipadasSpecTest | 16 | 0 |
| CorrecoesDeSegurancaSpecTest | 8 | 0 |
| CrudApiApplicationTests | 1 | 0 (1 pulado) |
| DossieDaEmpresaSpecTest | 15 | 0 |
| **EstadoDoAnuncioSpecTest** (spec 22) | 15 | 0 |
| FalhaDeTokenSpecTest | 10 | 0 |
| FavoritosEHistoricoSpecTest | 15 | 0 |
| **FiltrosDoAcervoSpecTest** (spec 18) | 15 | 0 |
| PaginacaoDasFilasSpecTest | 12 | 0 |
| PlanosConfiguraveisSpecTest | 15 | 0 |
| QuotaConfiguravelSpecTest | 1 | 0 |
| QuotaMensalDaPfSpecTest | 13 | 0 |
| SchemaVersionadoSpecTest | 9 | 0 |

### Os dois pontos de partida, confirmados por medição

1. **Os 4 erros de lint do front são exatamente os pré-existentes.** `npm run
   lint` acusa 4 erros e 6 avisos, e os 4 caem em `app/anuncie/page.tsx:58`,
   `app/contexts/auth-context.tsx:69`, `app/portal/perfil/page.tsx:47` e
   `components/ui/carousel.tsx:98`. Nenhum arquivo novo do ciclo aparece na
   lista. **Não há regressão de lint.**

2. **`FalhaDeTokenSpecTest` passou em todas as execuções desta homologação.**
   10/10 dentro da suíte completa, e 3/3 em execuções isoladas da classe. A
   instabilidade relatada (1 falha em 3) não reproduziu aqui — o que não a
   desmente: o teste troca o **último** caractere base64url da assinatura, e
   naquela posição parte dos bits não é significativa, então algumas trocas
   decodificam na mesma assinatura. Continua sendo um teste que pode reprovar
   sem ninguém ter mexido em nada.

---

## O que precisa da sua decisão

### O checklist arquitetural

Uma observação concreta por pergunta. **Nenhuma delas está respondida aqui** —
arquitetura é julgamento humano.

---

**Esta decisão segura se o volume triplicar?**

`GET /announcements/cidades` executa `select distinct a.city from Announcement a
where a.active = true order by a.city` (`AnnouncementRepository.cidades()`) sem
paginação e sem receber nenhum termo. A home chama essa rota na montagem, por
`useCidades()`, e o corte acontece no navegador: `CampoComSugestoes` filtra a
lista recebida e faz `.slice(0, 8)`. Hoje o acervo tem uma cidade; a rota
devolve a lista inteira a cada visita à home, e as migrações `V4` e `V5`
declaram, por escrito, que nenhuma das colunas normalizadas recebeu índice.

Do lado da tela de operação: `app/portal/anuncios/page.tsx` chama
`getMyAnnouncements()` sem `page` nem `size`, então vale o padrão do Spring —
20 por página — e a página não tem controle de paginação nenhum. A conta PJ, que
por decisão da spec 22 conta ativos e **não tem teto**, veria os 20 primeiros e
nenhum aviso de que há mais.

---

**Algum acoplamento novo preocupa a longo prazo?**

`ViaCepConsulta` entrou no caminho síncrono de `POST /announcements` e de
`PUT /announcements/{id}`: cada criação e cada edição pode gastar até 2s
esperando um serviço de terceiro, e o fracasso é registrado em `log.debug`
(`ViaCepConsulta.bairroDe`, bloco `catch`). O veredito da spec 23 já registra o
sintoma em ambiente real: *"`neighborhood` voltou nulo nos anúncios criados,
mesmo com CEP válido de Manaíra — o contêiner provavelmente não alcança o
ViaCEP"*. Ou seja, a integração já está falhando em silêncio no Docker, e nem a
tela nem o log em nível de erro dizem isso.

No front, o acoplamento é de tipo: `app/imoveis/page.tsx` faz
`const type = (parametros.get("type") as PropertyType | null) ?? undefined` — um
cast cru sobre texto de URL, sem validação. `?type=MANSAO` atravessa o hook e o
serviço até o backend, que responde 400 (é o C14 da spec 18), e no caminho
`propertyTypeLabel` indexa um `Record<PropertyType, string>` com uma chave que
não existe.

---

**A implementação diverge do plano em algum ponto não sinalizado?**

Sim, em um ponto, e ele atravessa os dois repositórios.

**O C2 da spec 19 não existe em lugar nenhum.** O critério pede que o bairro
resolvido pelo CEP apareça *no formulário, editável, antes de publicar*.
`grep -rn "neighborhood" app/anuncie/ app/schemas/` no front não devolve nada: o
formulário de anúncio não tem campo de bairro, e `app/schemas/announcement.ts`
não tem a propriedade. O veredito da spec 19 marcou o critério como "não
verificável nesta spec — defeito da spec, não do código" e concluiu *"falta a
tela"*; nenhuma das três specs do front deste ciclo (20, 21, 23) recolheu essa
tela. A consequência prática é que `AnnouncementCreateDTO.neighborhood` e
`AnnouncementUpdateDTO.neighborhood` estão no ar e **nada no produto os
preenche** — o que torna o C3 da mesma spec ("bairro digitado pelo anunciante
prevalece") verdadeiro no teste e inalcançável pela interface.

Duas divergências menores, também não sinalizadas:

- O plano da spec 23 previa `app/services/__tests__/announcements-me.test.ts`.
  Esse arquivo não existe; o que foi escrito no lugar foi
  `app/components/__tests__/linha-de-anuncio.test.tsx`.
- `8a70b32` mudou `SecurityConfig` e `AnnouncementListDTO` no back sem estar no
  plano de nenhuma das seis specs — é correção nascida na verificação da spec 23,
  no outro repositório.

E uma que **foi** sinalizada, duas vezes, e segue aberta: `AnnouncementService.edit`
recebe `email` e não o usa — faz `repository.findById(id)` e grava. Os vereditos
das specs 19 e 22 registraram. `PUT /api/v1/announcements/{id}` está no ar.

---

**Há dívida sendo criada conscientemente? Foi registrada?**

Registrada, em veredito: a PJ conta ativos e não tem teto (`temTeto` devolve
`false` para qualquer coisa que não seja `AccountType.PF`), enquanto
`Plan.limiteAnuncios` já existe e nada liga `Company` a um plano.

**Não registrada em lugar nenhum:** `/announcements/me/favoritos` e
`/announcements/me/historico` não filtram por `active`.
`FavoriteRepository.findAnnouncementsByUserEmail` é
`select f.announcement from Favorite f where f.user.email = :email order by
f.createdAt desc`, sem nenhuma condição sobre o estado do anúncio, e
`AnnouncementViewRepository` segue o mesmo desenho. Um anúncio tirado do ar pelo
dono continua listado — com título, preço e foto — nos favoritos e no histórico
de quem o tinha favoritado, enquanto `GET /announcements/{id}` devolve 404 para
essa mesma pessoa (`AnnouncementService.findOne`, que é o C8 da spec 22). A
lista afirma que o anúncio existe e o detalhe afirma que não.
`FavoritosEHistoricoSpecTest` passa com 15/15 porque é anterior ao estado do
anúncio e não tem como cobrir isso.

Também não registrada: `package-lock.json` (14.094 linhas, `lockfileVersion 3`)
foi commitado em `9d24505`, mas o `Dockerfile` do front continua com
`COPY package.json ./` seguido de `RUN npm install` (linhas 5-6), e o comentário
da linha 2 mais o `CLAUDE.md` ainda afirmam que não há lockfile no repositório.
O build do contêiner ignora o lock que o repo passou a ter, então o que roda em
Docker não é necessariamente o que a suíte mediu.

---

**Você assinaria embaixo disto como se tivesse escrito à mão?**

Só o humano responde.

---

### O que a suíte expôs e não é critério de nenhuma spec

A suíte completa **não** expôs falha. Os dois achados desta fase vieram de ler o
código depois de a medição estar fixada, e nenhum dos dois tem teste que os
pegue:

1. **Favoritos e histórico ignoram `active`** (detalhado acima). Anúncio fora do
   ar continua visível em lista para terceiros, e 404 no detalhe.
2. **`AnnouncementService.edit` não verifica o dono.** Já sinalizado em dois
   vereditos e ainda no ar.

Para os dois valem as três saídas de sempre: **corrigir**, **emendar a spec**, ou
**declarar fora de escopo**. O mesmo vale para o C2 órfão da spec 19, que hoje
não fecha nesta spec nem em nenhuma.

Nada foi corrigido nesta fase: nem código, nem spec, nem veredito.
