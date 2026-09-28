# 09-apoio-de-venda

## Intenção
Quem anuncia pede ajuda da equipe para vender mais rápido, e o pedido cai na fila do backoffice — hoje não existe canal entre anunciante e equipe.

## Depende de
01-contas-tipadas

## Critérios
- [ ] **C1** `dominio` — Dono do anúncio pede apoio e o pedido é criado.
- [ ] **C2** `seguranca` — Conta que não é dona do anúncio recebe 403 ao pedir apoio para ele.
- [ ] **C3** `seguranca` — Pedido de apoio sem autenticação responde 403.
- [ ] **C4** `dominio` — Pedido de apoio nasce com status `ABERTO`.
- [ ] **C5** `persistencia` — O pedido grava o anúncio e o solicitante.
- [ ] **C6** `dominio` — Segundo pedido para um anúncio que já tem pedido aberto é recusado com 409.
- [ ] **C7** `contrato-api` — Pedido de apoio para anúncio inexistente responde 404.
- [ ] **C8** `contrato-api` — `GET /api/v1/announcements/{id}/apoio` devolve o pedido vigente para o dono.
- [ ] **C9** `contrato-api` — `GET /api/v1/announcements/{id}/apoio` responde 404 quando não há pedido.
- [ ] **C10** `seguranca` — `GET /api/v1/backoffice/support` com papel diferente de `INTERNO` responde 403.
- [ ] **C11** `contrato-api` — `GET /api/v1/backoffice/support` lista pedido `ABERTO`.
- [ ] **C12** `contrato-api` — `GET /api/v1/backoffice/support` não lista pedido `CONCLUIDO`.
- [ ] **C13** `dominio` — Concluir o pedido pela equipe deixa o status `CONCLUIDO`.
- [ ] **C14** `persistencia` — Concluir grava o e-mail de quem atendeu e o instante.
- [ ] **C15** `dominio` — Com o pedido anterior concluído, o dono consegue abrir outro para o mesmo anúncio.

## Contrato técnico
- Entidade `SupportRequest`: anúncio, solicitante, `status`, `mensagem` opcional, `createdAt`, `handledBy`, `handledAt`.
- `enums/SupportStatus`: `ABERTO`, `CONCLUIDO`. Dois estados bastam; `EM_ATENDIMENTO` só teria valor se alguém precisasse saber que a equipe já pegou o caso, e ninguém pediu isso.
- **Qualquer dono de anúncio pode pedir apoio, PF ou PJ.** Foi decidido no gate que tipo de conta é identidade, não capacidade, e a autorização aqui é de posse do anúncio, não de tipo. Se o negócio quiser restringir à pessoa física, isso vira critério, não regra silenciosa.
- Só um pedido aberto por anúncio: dois pedidos abertos para o mesmo imóvel dividem o atendimento sem acrescentar informação.
- Quem não é dono recebe **403**, não 404. O anúncio é público, então esconder sua existência não protege nada, e 404 confundiria quem errou de conta.
- `handledBy` guarda o e-mail, como `Company.reviewedBy`, pelo mesmo motivo: a trilha tem de continuar legível se a conta interna sumir.
- As rotas de backoffice já nascem fechadas por papel desde a spec 01.

## Fora de escopo
- Conversa entre equipe e anunciante: o pedido é um sinal, não uma thread.
- Notificação ao anunciante de que o apoio foi concluído — depende do serviço de e-mail, adiado pelo gate.
- Telas do anunciante e do backoffice — spec de frontend própria.
- Limite de pedidos por conta ou por mês.
- Reabrir pedido concluído: o dono abre um novo, que é o que C15 garante.

## Plano
1. `enums/SupportStatus.java`.
2. `model/SupportRequest.java`.
3. `repositories/SupportRequestRepository.java` — `existsByAnnouncementIdAndStatus`, `findByAnnouncementIdAndStatus`, `findAllByStatus`.
4. `dtos/SupportRequestDTO.java`, `dtos/SupportRequestCreateDTO.java`, `dtos/BackofficeSupportListDTO.java`.
5. `services/SupportRequestService.java`.
6. `controllers/AnnouncementController.java` — `POST` e `GET` em `/{id}/apoio`.
7. `controllers/BackofficeController.java` — `GET /support` e `POST /support/{id}/concluir`.

Fatia única: a fila do backoffice depende da entidade e do serviço.

## Perguntas em aberto
nenhuma
