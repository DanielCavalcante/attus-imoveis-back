# 30-vinculo-de-plano-da-empresa

## Intenção
A empresa passa a ter um plano de verdade, liberado à mão enquanto não há
gateway, e o teto de anúncios dela passa a vir desse plano.

## Depende de
07-planos-configuraveis, 28-aprovacao-manda-no-anuncio

## Critérios

- [ ] **C1** `persistencia` — a migração cria o vínculo entre empresa e plano, com no máximo um plano vigente por empresa.
- [ ] **C2** `dominio` — empresa com plano de limite N não consegue ter o anúncio N+1 no ar.
- [ ] **C3** `dominio` — empresa com plano de limite nulo publica sem teto.
- [ ] **C4** `seguranca` — a rota que libera plano exige `ROLE_INTERNO`; conta comum recebe 403.
- [ ] **C5** `seguranca` — a empresa não tem rota para mudar o próprio plano.
- [ ] **C6** `contrato-api` — a rota de liberação vincula o plano informado à empresa e devolve o vínculo resultante.
- [ ] **C7** `dominio` — liberar plano para empresa que já tinha um substitui o vigente, sem criar um segundo.
- [ ] **C8** `dominio` — downgrade para plano de limite menor que o número de anúncios no ar não inativa nenhum anúncio.
- [ ] **C9** `dominio` — depois desse downgrade, os anúncios que já estavam no ar continuam aparecendo em `GET /announcements`.
- [ ] **C10** `dominio` — empresa acima do teto depois do downgrade recebe recusa ao tentar publicar mais um.
- [ ] **C11** `contrato-api` — antes de confirmar, o backoffice consegue ler quantos anúncios a empresa tem no ar e qual o limite do plano de destino.
- [ ] **C12** `persistencia` — o vínculo registra quem liberou e quando.
- [ ] **C13** `contrato-api` — `GET /announcements/quota` de conta PJ passa a refletir o limite do plano.
- [ ] **C14** `dominio` — liberar plano inativo é recusado: plano desativado não se vende nem se dá.

## Contrato técnico

O vínculo é entidade própria (`subscriptions`), não uma coluna `plan_id` em
`companies`: C12 pede quem liberou e quando, e histórico não cabe em coluna
que o próximo downgrade sobrescreve.

`AnnouncementService.temTeto` hoje responde "só PF". Ele passa a responder pelo
plano quando a conta é PJ, e a propriedade `attus.quota.pf.ativos` continua
valendo só para PF — a spec 06 não é revogada aqui.

C11 é leitura, não confirmação: o servidor informa o impacto, quem decide é a
pessoa no backoffice. É o que sustenta "avise, mas não bloqueie".

## Fora de escopo

- **Cobrança, gateway, fatura, vencimento.** O vínculo é manual por definição
  desta spec; data de expiração sem quem a renove viraria plano que cai sozinho.
- **Inativar anúncio excedente no downgrade** — você disse explicitamente que
  não, e C8 e C9 provam que não acontece.
- **A tela do backoffice** — é a spec 31, no front.
- **Plano para pessoa física.** PF tem a quota da spec 06 e não contrata plano.

## Plano

1. Entidade e migração do vínculo — `model/Subscription.java`,
   `src/main/resources/db/migration/V9__vinculo_de_plano.sql`.
2. Serviço de liberação, com a recusa de plano inativo —
   `services/SubscriptionService.java`, `repositories/SubscriptionRepository.java`.
3. Rotas no backoffice: liberar e consultar impacto —
   `controllers/BackofficeController.java`, `dtos/`.
4. `temTeto` e `quotaOf` passam a olhar o plano —
   `services/AnnouncementService.java`.
5. Testes por critério — `src/test/java/com/example/demo/VinculoDePlanoSpecTest.java`.

## Perguntas em aberto

- **Empresa aprovada e sem nenhum plano: pode publicar?** Muda C2 e o desenho
  de `temTeto`, e não é derivável — o repositório nunca ligou empresa a plano,
  então não há precedente para ler. As leituras possíveis são "não publica até
  ter plano", "publica com o teto da PF" e "publica sem teto". Insumo que
  falta: qual delas é a regra do negócio.
