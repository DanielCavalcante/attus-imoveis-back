# 19-bairro-derivado-do-cep

## Intenção
Quem procura imóvel recorta por bairro, e ninguém precisa cadastrar bairro nenhum: ele sai do CEP que o anunciante já digita.

## Depende de
18-filtros-do-acervo

## Critérios
- [ ] **C1** `integracao` — Anúncio criado com CEP que a consulta resolve grava o bairro sem o anunciante digitá-lo.
- [ ] **C2** `experiencia` — O bairro resolvido aparece no formulário, em campo editável, antes de publicar.
- [ ] **C3** `dominio` — Bairro digitado pelo anunciante prevalece sobre o resolvido pelo CEP.
- [ ] **C4** `integracao` — CEP que não resolve bairro não impede a publicação do anúncio.
- [ ] **C5** `integracao` — Serviço de CEP indisponível não impede a publicação do anúncio.
- [ ] **C6** `dominio` — Anúncio sem bairro não aparece em nenhum filtro de bairro.
- [ ] **C7** `dominio` — `neighborhood=Manaíra` devolve apenas anúncios daquele bairro.
- [ ] **C8** `dominio` — `neighborhood=manaira` devolve o anúncio cujo bairro é "Manaíra".
- [ ] **C9** `dominio` — `neighborhood` combinado com `city` devolve apenas anúncios que satisfazem os dois.
- [ ] **C10** `contrato-api` — Existe rota que lista os bairros de uma cidade.
- [ ] **C11** `dominio` — Essa lista traz apenas bairros com ao menos um anúncio publicado.
- [ ] **C12** `dominio` — Essa lista não repete bairro.
- [ ] **C13** `dominio` — A rota de cidades traz apenas cidades com ao menos um anúncio publicado.
- [ ] **C14** `persistencia` — Uma rotina de carga única preenche o bairro dos anúncios já publicados a partir do CEP gravado.
- [ ] **C15** `persistencia` — Anúncio cujo CEP não resolve continua sem bairro depois dessa rotina.

## Contrato técnico
- **Nenhuma tabela de bairros, e é isto que faz a coisa escalar.** A lista de bairros de uma cidade é `distinct` sobre os anúncios daquela cidade. O sistema nasce sabendo os bairros de João Pessoa porque é lá que há anúncio, e sabe os de Belo Horizonte no dia em que alguém publicar lá — sem ninguém cadastrar nada. Tabela de cadastro exigiria manter 5.570 municípios à mão, que é exatamente o custo que esta spec existe para não pagar.
- **A mesma regra vale para cidades**, e por isso a rota de cidades entra aqui: o filtro de cidade da spec 18 sabe filtrar, mas não sabe dizer quais cidades existem.
- **A consulta de CEP roda no servidor, no momento de criar e de atualizar.** No navegador ela ficaria fora de controle de tempo limite e de teste, e o valor gravado precisa ser o mesmo que o filtro usa.
- **Tempo limite curto e falha silenciosa.** Bairro é recorte de busca, não dado essencial do imóvel: derrubar a publicação de um anúncio porque um serviço de terceiro caiu troca um problema pequeno por um grande. Sem resposta, o anúncio publica sem bairro e some dos filtros de bairro — C6 é o que prova que isso não vira lixo no acervo.
- **O anunciante vence o CEP.** Cidade com CEP único devolve bairro vazio ou errado, e quem conhece o endereço é quem mora nele. Por isso C2 mostra o valor antes de publicar em vez de gravá-lo escondido.
- Coluna `neighborhood` mais `neighborhood_search` normalizada, preenchidas no mesmo método da entidade que já mantém `search_text` e `city_search`. Acento não pode importar, pela razão que a spec 16 fixou.
- **A carga dos anúncios antigos não é da migração.** Flyway não faz chamada HTTP: a migração cria as colunas e a rotina de carga roda uma vez, fora dela. Os anúncios já publicados têm CEP gravado, então ninguém preenche nada à mão.
- Dependência externa nova e declarada: um serviço público de consulta de CEP. Ele fica atrás de uma interface própria, para que o teste não dependa da rede e para que trocá-lo depois não espalhe.

## Fora de escopo
- Cadastro manual de bairros e qualquer tela para administrá-los — é o custo que esta spec recusa.
- Validação de que o bairro digitado existe de fato: o anunciante é a autoridade sobre o próprio endereço.
- Correção de bairro em anúncio já publicado por rotina automática depois da carga inicial.
- Normalização de grafias divergentes do mesmo bairro ("Jardim Oceania" e "Jd. Oceania" ficam separados).
- Busca por proximidade, raio ou coordenada.
- **A tela.** Os controles de filtro em `/imoveis` e o campo no formulário de anúncio são a spec seguinte, no repositório do front.

## Plano
1. `src/main/resources/db/migration/V5__bairro_do_anuncio.sql` — colunas `neighborhood` e `neighborhood_search`.
2. `integracoes/ConsultaDeCep.java` — interface e implementação sobre o serviço público, com tempo limite.
3. `model/Announcement.java` — `neighborhood` e a normalização no método que já mantém `search_text` e `city_search`.
4. `services/AnnouncementService.java` — resolve o bairro na criação e na atualização; valor informado prevalece.
5. `repositories/AnnouncementRepository.java` — filtro por bairro e as duas consultas `distinct` de bairros e cidades.
6. `controllers/AnnouncementController.java` — parâmetro `neighborhood` e as rotas de bairros e cidades.
7. `services/CargaDeBairros.java` — rotina única sobre os anúncios sem bairro.
8. `src/test/java/com/example/demo/BairroDoAnuncioSpecTest.java`.

Fatia única: a derivação, o filtro e a lista são o mesmo dado, e C6 só se prova com os três no lugar.

## Perguntas em aberto
nenhuma
