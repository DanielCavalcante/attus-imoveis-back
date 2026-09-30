# 10-favoritos-e-historico

## Intenção
Quem navega guarda os imóveis que interessam e reencontra o que já viu, de qualquer dispositivo.

## Depende de
01-contas-tipadas

> **Aprovada no gate humano em 2026-09-28.** Todos os critérios aceitos.

## Critérios
- [x] **C1** `dominio` — Favoritar um anúncio cria o favorito para a conta autenticada.
- [x] **C2** `dominio` — Favoritar o mesmo anúncio duas vezes não cria um segundo favorito.
- [x] **C3** `dominio` — Desfavoritar remove o favorito da conta.
- [x] **C4** `contrato-api` — `GET /api/v1/announcements/me/favoritos` lista os anúncios favoritados da conta.
- [x] **C5** `dominio` — Favorito de outra conta não aparece na lista desta.
- [x] **C6** `seguranca` — Favoritar sem autenticação responde 403.
- [x] **C7** `contrato-api` — Favoritar anúncio inexistente responde 404.
- [x] **C8** `contrato-api` — A listagem de favoritos traz título, cidade, preço e imagem de cada anúncio.
- [x] **C9** `dominio` — Visitar o detalhe de um anúncio com sessão registra a visualização.
- [x] **C10** `dominio` — Visitar o mesmo anúncio de novo não cria uma segunda linha de histórico.
- [x] **C11** `dominio` — Visitar o mesmo anúncio de novo atualiza o instante da visita.
- [x] **C12** `dominio` — Visita sem sessão não registra histórico.
- [x] **C13** `contrato-api` — `GET /api/v1/announcements/me/historico` lista do mais recente para o mais antigo.
- [x] **C14** `seguranca` — `GET /api/v1/announcements/me/historico` sem autenticação responde 403.
- [x] **C15** `persistencia` — Remover um anúncio remove os favoritos e o histórico dele.

## Contrato técnico
- Duas entidades: `Favorite` e `AnnouncementView`, ambas com par único `(user, announcement)`.
- **Histórico no servidor, não em `localStorage`.** Decidido no gate: histórico que some ao trocar de dispositivo não é histórico. O custo é uma escrita por visita de anúncio autenticada.
- Visita repetida é **atualização do instante**, não linha nova. A pergunta que o histórico responde é "o que eu já vi e quando foi a última vez", não "quantas vezes abri".
- Favoritar duas vezes é **idempotente**, não conflito: o botão de favoritar é um alternador, e um duplo clique não é erro do usuário.
- O registro de visita acontece em `GET /api/v1/announcements/{id}`, que é público. Com token presente o filtro popula o contexto e a visita é gravada; sem token, `@AuthenticationPrincipal` chega nulo e nada é gravado. **A leitura do anúncio nunca falha por causa do histórico.**
- `Announcement` ganha as duas coleções com cascade, pelo mesmo motivo das specs 02 e 09: chave estrangeira sem cascade transforma remoção em 500.
- As listagens reaproveitam `AnnouncementListDTO`, que já é a forma do card.

## Fora de escopo
- Contato com o anunciante — spec 11; é a terceira parte do pedido e não cabe aqui em critério atômico.
- Limpar o histórico ou remover item dele.
- Limite de tamanho do histórico e expurgo por idade.
- Contagem de visualizações do anúncio para o anunciante: isto aqui é o histórico de quem vê, não a métrica de quem publica.
- Telas de favoritos e histórico — spec de frontend própria.

## Plano
1. `model/Favorite.java` e `model/AnnouncementView.java`.
2. `repositories/FavoriteRepository.java` e `repositories/AnnouncementViewRepository.java`.
3. `model/Announcement.java` — coleções com cascade.
4. `services/UserLibraryService.java` — favoritar, desfavoritar, registrar visita e as duas listagens.
5. `controllers/AnnouncementController.java` — `POST` e `DELETE` em `/{id}/favorito`, `GET /me/favoritos`, `GET /me/historico`, e o registro de visita no detalhe.

Fatia única: as duas listagens dependem das entidades e do serviço.

## Perguntas em aberto
nenhuma
