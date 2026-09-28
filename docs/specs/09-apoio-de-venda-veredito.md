# Veredito — 09-apoio-de-venda

Base: estado da árvore após a spec 07, mais os arquivos novos de `.` (nada commitado desde `1e03b12`).

> **Limitação de independência.** Produzido pela mesma sessão que escreveu o código, a pedido do humano. O passo 2 do `verificar` pede contexto limpo porque quem escreve o código escreve os testes. Leia sabendo que essa garantia não existe aqui.

- **C1** — atendido. `SupportRequestService.open` grava o pedido depois de confirmar a posse; o teste conta a linha no banco em vez de confiar no corpo da resposta.

- **C2** — atendido. `requireOwnedAnnouncement` compara o e-mail do dono com o do token e lança `AccessDeniedException`, que passou a ter handler devolvendo 403.

- **C3** — atendido. `POST /api/v1/announcements/{id}/apoio` não está entre as rotas liberadas — o `permitAll` de announcements cobre só `GET` — então cai em `anyRequest().authenticated()`.

- **C4** — atendido. `open` grava `ABERTO` explicitamente, sem depender do valor padrão do campo.

- **C5** — atendido. O teste relê o pedido e compara o id do anúncio e o id do solicitante. Os ids saem do proxy lazy sem abrir sessão; comparar por e-mail exigiria inicializar o proxy e mediria o teste, não o código.

- **C6** — atendido. `existsByAnnouncementIdAndStatus(id, ABERTO)` antes da gravação lança `ConflictException` → 409.

- **C7** — atendido. `requireOwnedAnnouncement` busca o anúncio primeiro e lança `ResourceNotFoundException` → 404, antes de qualquer checagem de posse.

- **C8** — atendido. `findOpen` devolve o pedido `ABERTO` do anúncio e o teste casa o status no JSON.

- **C9** — atendido. Sem pedido aberto, `findOpen` lança `ResourceNotFoundException` → 404. Distinguir de C8 importa: a tela precisa saber se mostra "pedir apoio" ou "apoio solicitado".

- **C10** — atendido. `/api/v1/backoffice/**` exige `hasRole("INTERNO")` desde a spec 01.

- **C11** — atendido. A fila devolve `announcementId` e `solicitanteEmail`, além de título, cidade, nome e telefone — o suficiente para a equipe ligar para alguém sem uma segunda consulta.

- **C12** — atendido. `findAllByStatus(ABERTO)` exclui o concluído, e o teste confirma lista vazia depois da conclusão.

- **C13** — atendido. `conclude` grava `CONCLUIDO` e o teste relê do banco.

- **C14** — atendido. `conclude` grava `handledBy` com o e-mail do token e `handledAt` com o instante; o teste cria a conta interna e compara o e-mail.

- **C15** — atendido. Como a checagem de duplicidade filtra por status `ABERTO`, o pedido concluído não bloqueia um novo.

## Observações que não são critério

- **Regressão corrigida no meio desta spec.** A chave estrangeira de `support_requests` para `announcements` quebrou a remoção em cascata, e a suíte inteira reprovou. Ao investigar, a mesma falha já existia entre `users` e `announcements` desde antes: `DELETE /api/v1/users/{id}` numa conta com anúncio devolvia 500. Ambas foram resolvidas com cascade e `orphanRemoval` nas duas coleções.
- Concluir não valida o estado de origem: a equipe consegue concluir um pedido já concluído, o que sobrescreve `handledBy` e `handledAt`. A fila só mostra abertos, então pela interface não acontece.
- A fila devolve tudo, sem paginação nem ordenação — mesma limitação da fila de análise de empresas.
- O pedido guarda `mensagem` mas nada a exibe para quem atende além da própria fila; não existe resposta ou histórico, por decisão declarada no fora de escopo.
- Qualquer dono de anúncio pode pedir apoio, PF ou PJ, conforme o contrato técnico. Se o negócio quiser restringir à pessoa física, isso ainda não foi decidido.
