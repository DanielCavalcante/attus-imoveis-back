# 34-varias-imagens-por-anuncio

## Intenção
O anúncio passa a ter um conjunto de fotos, com uma delas escolhida como capa.

## Depende de
32-imagem-enviada-e-nao-linkada

## Critérios

- [ ] **C1** `persistencia` — a migração cria a tabela de imagens do anúncio, guardando a ordem de cada uma.
- [ ] **C2** `dominio` — criar anúncio com várias imagens grava todas elas.
- [ ] **C3** `dominio` — a ordem em que as imagens foram enviadas é a ordem em que voltam.
- [ ] **C4** `dominio` — um anúncio com várias imagens tem exatamente uma capa.
- [ ] **C5** `dominio` — sem capa indicada, a primeira da lista vira a capa.
- [ ] **C6** `contrato-api` — o detalhe do anúncio devolve todas as imagens.
- [ ] **C7** `contrato-api` — a listagem continua devolvendo uma imagem em `image`, e ela é a capa.
- [ ] **C8** `dominio` — editar o anúncio troca a capa sem alterar a ordem das demais.
- [ ] **C9** `dominio` — remover uma imagem na edição apaga também o objeto no bucket.
- [ ] **C10** `dominio` — passar do teto de imagens por anúncio é recusado.
- [ ] **C11** `seguranca` — anexar ao anúncio uma imagem enviada por outra conta é recusado.
- [ ] **C12** `dominio` — excluir o anúncio apaga os objetos dele no bucket.
- [ ] **C13** `dominio` — anúncio gravado antes desta spec responde com uma imagem só, a que ele já tinha.
- [ ] **C14** `dominio` — requisição recusada não deixa nem linha na tabela nem objeto no bucket.

## Contrato técnico

Tabela própria (`announcement_images`), com `posicao` inteiro e `capa`
booleano. Ordem em coluna e não implícita pelo id: reordenar é caso normal, e
ordem que depende da chave primária não se reordena sem reescrever linha.

`image` na tabela `announcements` **não sai**. Ela passa a guardar a capa,
espelhada, e é isso que faz C7 e C13 serem verdade ao mesmo tempo: a listagem
não muda de forma, e o anúncio antigo continua tendo uma imagem sem precisar
de migração de dados. Espelho tem custo -- duas fontes para o mesmo fato -- e o
que o paga é a gravação passar por um lugar só, no serviço.

A capa é o único lugar onde a escolha é obrigatória: card, busca e
compartilhamento precisam de uma imagem, e "a primeira" só é resposta certa
por acaso.

C11 exige que a imagem enviada guarde quem a enviou. Sem isso, o identificador
devolvido pela spec 32 vira referência que qualquer conta anexa ao próprio
anúncio -- a foto de um imóvel de terceiro anunciada como sua.

Teto de 12 imagens por anúncio, em propriedade.

## Fora de escopo

- **Vídeo, planta baixa ou tour.** São outros tipos de mídia, com outras
  regras de tamanho e reprodução.
- **Reordenar arrastando na tela** — a ordem existe na API por C3; o gesto é
  da spec 35, se couber lá.
- **Legenda ou descrição por foto.**
- **Apagar do bucket o que ficou órfão de um envio que nunca virou anúncio.**
  É faxina periódica, e vira spec própria com a decisão de prazo junto.

## Plano

1. Entidade, migração e teto — `model/AnnouncementImage.java`,
   `db/migration/V10__imagens_do_anuncio.sql`, `application.properties`.
2. Dono da imagem enviada, para a checagem da C11 — `services/ImagemService.java`.
3. Gravação do conjunto, capa e espelho — `services/AnnouncementService.java`.
4. Contrato de entrada e saída — `dtos/AnnouncementCreateDTO.java`,
   `dtos/AnnouncementUpdateDTO.java`, `dtos/AnnouncementDetailDTO.java`.
5. Apagar objetos ao excluir e ao remover — `services/AnnouncementService.java`.
6. Testes por critério — `src/test/java/com/example/demo/ImagensDoAnuncioSpecTest.java`.

## Perguntas em aberto

Nenhuma.
