# 24-coerencia-do-anuncio-fora-do-ar

## Intenção
O que está fora do ar some de todo lugar que lista anúncio, e editar anúncio passa a exigir ser dono dele.

## Depende de
22-anuncio-ativo-e-quota-por-ativos

> **Aprovada no gate humano em 2026-09-29.** Todos os critérios aceitos.

## Critérios
- [ ] **C1** `dominio` — Anúncio inativado não aparece na lista de favoritos de quem o favoritou.
- [ ] **C2** `dominio` — O favorito não é apagado: reativado o anúncio, ele volta à lista.
- [ ] **C3** `dominio` — O total da página de favoritos não conta anúncio fora do ar.
- [ ] **C4** `dominio` — Anúncio inativado não aparece no histórico de visitas.
- [ ] **C5** `dominio` — Reativado, ele volta ao histórico.
- [ ] **C6** `dominio` — O total da página de histórico não conta anúncio fora do ar.
- [ ] **C7** `dominio` — Favoritar anúncio fora do ar é recusado.
- [ ] **C8** `seguranca` — Editar anúncio de outra conta é recusado.
- [ ] **C9** `seguranca` — A recusa da edição não revela que o anúncio existe.
- [ ] **C10** `dominio` — Editar o próprio anúncio continua funcionando.

## Contrato técnico
- **A inconsistência que esta spec fecha é de estado, não de permissão.** Desde a spec 22 o anúncio fora do ar responde 404 no detalhe para quem não é dono, mas `FavoriteRepository.findAnnouncementsByUserEmail` e `AnnouncementViewRepository.findAnnouncementsByUserEmail` não filtram estado: a lista diz que o imóvel existe, com título, preço e foto, e o detalhe diz que não. Nenhuma das duas suítes pegou porque as duas são anteriores ao estado do anúncio.
- **Some da lista, mas o registro fica.** O favorito e a visita são da pessoa, não do anunciante: apagá-los porque o dono tirou o anúncio do ar destruiria dado de terceiro. Some da consulta e volta quando o anúncio voltar — é o que C2 e C5 provam.
- **O `countQuery` filtra junto.** Filtrar só a consulta principal daria página com menos itens do que o total anunciado, e paginação que não fecha a conta é pior que lista errada: quem navega descobre páginas vazias no fim.
- **A alternativa que não foi escolhida:** mostrar o favorito marcado como "fora do ar". Preserva mais informação para quem favoritou, mas exige que o detalhe passe a servir anúncio inativo a quem favoritou — reabrindo o C8 da spec 22, que existe para link antigo não continuar servindo a página como se nada tivesse mudado.
- **`edit` nasceu sem dono verificado e a rota está no ar.** `AnnouncementService.edit` recebe o e-mail da sessão e não o usa: faz `findById` e grava. Qualquer conta autenticada edita anúncio de qualquer outra. A spec 22 corrigiu o mesmo padrão em inativar, reativar e excluir e deixou este de fora por escopo estrito; a homologação do ciclo 02 sinalizou pela terceira vez.
- **404 e não 403 para quem não é dono**, pela mesma razão das outras três rotas: responder "existe, mas não é seu" conta ao curioso que aquele id existe. C9 é o critério que fixa isso.

## Fora de escopo
- Avisar quem favoritou que o anúncio saiu do ar. É notificação, e o produto não envia mensagem nenhuma.
- Limpar favoritos e visitas de anúncios excluídos: a exclusão já remove por cascata.
- Índice sobre `active` nas consultas de favorito e histórico — índice sem medição é adivinhação, e a homologação do ciclo 02 levantou a questão de volume sem número.
- Permitir que a equipe interna edite anúncio de terceiro: hoje ninguém faz isso, e criar o poder é demanda própria.

## Plano
1. `repositories/FavoriteRepository.java` — estado na consulta e no `countQuery`.
2. `repositories/AnnouncementViewRepository.java` — o mesmo.
3. `services/UserLibraryService.java` — recusa de favoritar anúncio fora do ar.
4. `services/AnnouncementService.java` — `edit` passa a usar `doDono`, que já existe desde a spec 22.
5. `src/test/java/com/example/demo/CoerenciaDoAnuncioForaDoArSpecTest.java`.

Fatia única: C2 e C5 só se provam com o filtro e a reativação no lugar ao mesmo tempo.

## Perguntas em aberto
nenhuma
