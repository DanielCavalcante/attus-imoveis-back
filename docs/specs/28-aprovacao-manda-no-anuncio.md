# 28-aprovacao-manda-no-anuncio

## Intenção
Empresa que ainda não passou pela análise para de publicar imóvel no site.

## Depende de
05-analise-de-confiabilidade

## Critérios

- [ ] **C1** `seguranca` — `POST /announcements` de conta PJ cuja empresa está em `EM_ANALISE` é recusado com 403.
- [ ] **C2** `seguranca` — o mesmo POST é recusado quando a empresa está em `NAO_ENVIADO`.
- [ ] **C3** `seguranca` — o mesmo POST é recusado quando a empresa está em `REPROVADA`.
- [ ] **C4** `dominio` — conta PJ com empresa `APROVADA` publica e recebe 200.
- [ ] **C5** `dominio` — conta PF publica sem que o estado de análise seja consultado.
- [ ] **C6** `dominio` — a recusa não grava nada: o acervo da empresa continua com a mesma contagem de antes da tentativa.
- [ ] **C7** `contrato-api` — o corpo do 403 nomeia o estado da análise, e não só "não autorizado".
- [ ] **C8** `seguranca` — reativar anúncio inativo de PJ não aprovada é recusado pela mesma regra: voltar ao ar é publicar.
- [ ] **C9** `dominio` — editar anúncio que já existe continua permitido para PJ não aprovada.
- [ ] **C10** `dominio` — anúncio que a empresa publicou antes desta regra continua ativo e continua aparecendo em `GET /announcements`.
- [ ] **C11** `contrato-api` — `GET /announcements/quota` passa a dizer se a conta pode publicar agora e, quando não pode, por quê.
- [ ] **C12** `seguranca` — a recusa vem do servidor: requisição direta, sem passar pela tela, recebe o mesmo 403.

## Contrato técnico

403 e não 409: 409 significa "falta vaga, abra uma" e essa leitura é falsa aqui
— nenhuma vaga resolve, só a aprovação. É a mesma distinção que a spec 22 fixou
para a quota, no sentido contrário.

O campo de C11 é acrescentado a `AnnouncementQuotaDTO`, que a tela já consome;
rota nova para dizer a mesma coisa seria uma segunda fonte de verdade.

## Fora de escopo

- **Tirar do ar o que já foi publicado sem aprovação.** A regra vale para
  publicação nova. Derrubar acervo existente é decisão de produto com efeito
  sobre quem já anuncia, e você fixou o princípio na outra metade deste pedido
  ("não bloqueie nem altere anúncios"); C10 registra a escolha para ela ficar
  visível no gate, e não escondida numa omissão.
- **A tela que explica a recusa** — é a spec 29, no front.
- **Qualquer relação entre empresa e plano** — é a spec 30. Aprovação e plano
  são portões diferentes, e juntá-los numa spec só faria nenhum dos dois fechar.

## Plano

1. `AnnouncementService.ensureQuota` ganha um irmão, `ensurePodePublicar`, que
   consulta a empresa do dono — `services/AnnouncementService.java`,
   `repositories/CompanyRepository.java`.
2. Chamar o novo guarda em `create` e em `reativar`, antes de qualquer gravação
   — `services/AnnouncementService.java`.
3. O motivo da recusa entra em `AnnouncementQuotaDTO` — `dtos/`.
4. Exceção de 403 com mensagem que nomeia o estado —
   `exceptions/custom/`, `exceptions/` (handler).
5. Testes por critério — `src/test/java/com/example/demo/AprovacaoDoAnuncioSpecTest.java`.

## Perguntas em aberto

Nenhuma.
