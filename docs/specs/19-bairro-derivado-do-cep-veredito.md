# 19-bairro-derivado-do-cep — veredito

Medido contra `8dbbfe5..HEAD`, escopo `.`, com `BairroDoAnuncioSpecTest` (14/14)
mais as cinco suítes que compartilham criação e edição de anúncio (87 no total,
todas verdes).

São 14 testes para 15 critérios, e a diferença é o C2 — ver abaixo.

- **C1** — atendido. Com o CEP 58038-000 mapeado para "Manaíra" no dublê e o campo de bairro ausente no payload, o anúncio gravado sai com `neighborhood = "Manaíra"`.
- **C2** — **não verificável nesta spec.** O critério exige que o bairro resolvido apareça **no formulário**, editável, antes de publicar; a seção "Fora de escopo" da mesma spec exclui a tela e manda o campo do formulário para uma spec do front. Os dois não podem ser verdade ao mesmo tempo: **é defeito da spec, não do código.** O backend entrega o que torna o critério satisfazível adiante — `neighborhood` entra no `AnnouncementCreateDTO`, no `AnnouncementUpdateDTO`, no `AnnouncementDetailDTO` e no `AnnouncementListDTO`, e o valor informado prevalece (provado em C3). Falta a tela.
- **C3** — atendido. Com o CEP resolvendo "Manaíra" e o payload informando "Tambaú", o gravado é "Tambaú". O informado vence antes de a consulta ser sequer chamada.
- **C4** — atendido. CEP não mapeado no dublê: a criação responde 200 e o anúncio fica com `neighborhood` nulo.
- **C5** — atendido. Com o dublê em `foraDoAr`, a criação responde 200 e o bairro fica nulo. A publicação não depende do serviço de terceiro.
- **C6** — atendido. Dois anúncios na mesma cidade, um com bairro e outro sem: `neighborhood=Manaíra` devolve só o que tem. Anúncio sem bairro não vira lixo no acervo, apenas não responde ao filtro.
- **C7** — atendido. Manaíra e Bessa na mesma cidade: o filtro devolve um só, e o `neighborhood` do resultado confere.
- **C8** — atendido. `neighborhood=manaira` encontra "Manaíra". A coluna `neighborhood_search` é preenchida pela mesma normalização NFD de `search_text` e `city_search`.
- **C9** — atendido. "Centro" existe em João Pessoa e em Recife; com `city` mais `neighborhood` volta um só, e é o de João Pessoa. Interseção, não união.
- **C10** — atendido. `GET /api/v1/announcements/bairros?city=João Pessoa` responde 200 com a lista.
- **C11** — atendido. Com um anúncio em João Pessoa e outro em Recife, a lista de João Pessoa tem tamanho 1 e traz só "Manaíra".
- **C12** — atendido. Dois anúncios em Manaíra e um no Bessa produzem lista de tamanho 2. O `distinct` está na consulta, não em filtragem posterior.
- **C13** — atendido. Dois anúncios na mesma cidade produzem lista de cidades de tamanho 1.
- **C14** — atendido. Anúncio criado sem bairro, depois o CEP passa a resolver: `CargaDeBairros.executar()` devolve 1 e o anúncio fica com "Manaíra". A rotina é idempotente — só olha quem está sem bairro.
- **C15** — atendido. Anúncio com CEP que não resolve: a carga devolve 0 e o bairro continua nulo. Nada é inventado.

## Observações que não são critério

- **Defeito de spec em C2**, descrito acima. Precisa de decisão humana: ou o critério migra para a spec do front, ou a spec 19 deixa de excluir a tela. Como está, ele não fecha nesta spec nem em nenhuma.
- **Achado de segurança fora do escopo desta spec:** `AnnouncementService.edit` recebe o e-mail da sessão mas **não o usa** — faz `repository.findById(id)` e grava. Qualquer conta autenticada edita anúncio de qualquer outra. É o mesmo padrão do `@DeleteMapping` comentado que a spec 22 vai corrigir, mas no `edit` a rota **está no ar**. Não corrigi porque escopo estrito é regra do método; precisa de spec própria, ou entrar na 22.
- O acoplamento novo com serviço externo fica atrás de `ConsultaDeCep`, e a suíte inteira roda sem rede: o dublê é injetado via `@TestConfiguration` com `@Primary`. A implementação real tem tempo limite de 2s e a chave `attus.cep.viacep.habilitada` para desligar.
- A carga inicial não roda em teste nem por padrão: `attus.bairros.carga-inicial` nasce `false`. O teste chama `executar()` direto, que é o que prova C14 e C15 sem depender do ciclo de start.
