# 25-catalogo-de-bairros

## Intenção
A lista de bairros de João Pessoa passa a ser a cidade inteira, e não só os bairros onde por acaso já existe anúncio.

## Depende de
19-bairro-derivado-do-cep

## Critérios
- [ ] **C1** `persistencia` — Existe tabela de bairros por cidade.
- [ ] **C2** `persistencia` — A migração semeia os bairros de João Pessoa a partir de fonte oficial.
- [ ] **C3** `persistencia` — O mesmo nome de bairro não entra duas vezes na mesma cidade.
- [ ] **C4** `dominio` — A lista de bairros de João Pessoa inclui bairro que não tem nenhum anúncio.
- [ ] **C5** `contrato-api` — A rota de bairros responde no mesmo endereço e no mesmo formato de antes.
- [ ] **C6** `dominio` — A lista vem em ordem alfabética.
- [ ] **C7** `dominio` — A lista não repete bairro.
- [ ] **C8** `dominio` — Cidade sem catálogo devolve os bairros que têm anúncio.
- [ ] **C9** `dominio` — Filtrar por bairro do catálogo que tem anúncio devolve aquele anúncio.
- [ ] **C10** `dominio` — Filtrar por bairro do catálogo sem anúncio devolve página vazia com total zero.

## Contrato técnico
- **Esta spec reverte a decisão central da spec 19, e isso é escolha de produto, não refactor.** Lá está escrito: "nenhuma tabela de bairros, e é isto que faz a coisa escalar"; a lista era `distinct` sobre os anúncios e o custo recusado era manter 5.570 municípios à mão. A reversão vale porque o escopo mudou: **uma cidade**, com lista pequena, estável e pública. O argumento da spec 19 continua de pé para o Brasil inteiro — por isso o catálogo é por cidade e opcional, não obrigatório.
- **O bairro do anúncio continua vindo do CEP.** Nada na spec 19 muda quanto a isso: o catálogo alimenta as **opções de filtro**, não o dado do anúncio. Um anúncio pode ter bairro que não está no catálogo, e continua encontrável por ele.
- **Cidade sem catálogo mantém o comportamento atual.** É o que faz a mudança não quebrar Recife nem nenhuma outra: com catálogo, o catálogo manda; sem catálogo, o `distinct` dos anúncios. C8 é o critério que prende isso.
- **Opção sem resultado é consequência aceita, não defeito.** Listar a cidade inteira significa oferecer bairros onde ainda não há imóvel; escolher um devolve lista vazia, e a tela já nomeia isso. O ganho é quem procura descobrir que o bairro existe no produto — e o custo de esconder seria a pessoa concluir que o site não cobre o bairro dela.
- **A semente vem de fonte oficial e não é inventada.** Os bairros de João Pessoa são definidos por lei municipal e a lista é pública. Se a lista não puder ser obtida de fonte verificável na hora de codificar, **esta spec não fecha**: preencher a migração com nomes prováveis produziria filtro que mente sobre a geografia da cidade. Melhor não fechar do que semear adivinhação.
- Migração `V7`, sob a disciplina da spec 12: tabela, restrição de unicidade por cidade mais nome, e a carga.
- A comparação é pela forma normalizada, mesma de `city_search` e `neighborhood_search`: acento e caixa não podem separar "Manaíra" de "manaira".

## Fora de escopo
- Catálogo de outras cidades. A regra fica pronta para receber, mas semear é demanda por cidade.
- Tela interna para administrar bairros: lista estável de uma cidade não justifica CRUD.
- Corrigir anúncio cujo bairro não está no catálogo — o anunciante é a autoridade sobre o próprio endereço, como a spec 19 fixou.
- Unificar grafias divergentes já gravadas nos anúncios.
- **A tela.** O seletor pesquisável é spec própria, no repositório do front.

## Plano
1. `src/main/resources/db/migration/V7__catalogo_de_bairros.sql` — tabela, unicidade e a carga de João Pessoa.
2. `model/Neighborhood.java` — a entidade, com a forma normalizada mantida como nas demais.
3. `repositories/NeighborhoodRepository.java` — bairros de uma cidade, ordenados.
4. `services/AnnouncementService.java` — `bairrosDe` consulta o catálogo e cai no `distinct` quando a cidade não tem catálogo.
5. `src/test/java/com/example/demo/CatalogoDeBairrosSpecTest.java`.

Fatia única: C4 e C8 são os dois lados da mesma decisão e não se provam separados.

## Perguntas em aberto
nenhuma
