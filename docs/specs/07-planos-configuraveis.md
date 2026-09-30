# 07-planos-configuraveis

## Intenção
A equipe interna cria e ajusta os pacotes vendidos à pessoa jurídica pelo painel, e quem visita vê os planos disponíveis — hoje não existe plano algum.

## Depende de
01-contas-tipadas

> **Aprovada no gate humano em 2026-09-28.** Todos os critérios aceitos.

## Critérios
- [x] **C1** `seguranca` — `POST /api/v1/backoffice/plans` com papel diferente de `INTERNO` responde 403.
- [x] **C2** `contrato-api` — `POST /api/v1/backoffice/plans` com papel `INTERNO` responde 201.
- [x] **C3** `persistencia` — O plano criado fica gravado com nome, preço mensal e limite de anúncios.
- [x] **C4** `contrato-api` — Criar plano sem nome é recusado com 400 e a mensagem nomeia o campo.
- [x] **C5** `contrato-api` — Criar plano com preço mensal negativo é recusado com 400.
- [x] **C6** `persistencia` — Criar plano com nome já existente é recusado com 409.
- [x] **C7** `dominio` — Plano criado nasce ativo.
- [x] **C8** `persistencia` — `PUT /api/v1/backoffice/plans/{id}` grava o novo preço mensal.
- [x] **C9** `seguranca` — `PUT /api/v1/backoffice/plans/{id}` com papel diferente de `INTERNO` responde 403.
- [x] **C10** `dominio` — Desativar um plano deixa-o inativo.
- [x] **C11** `persistencia` — Plano desativado continua existindo no banco.
- [x] **C12** `contrato-api` — `GET /api/v1/backoffice/plans` lista também os planos inativos.
- [x] **C13** `contrato-api` — `GET /api/v1/plans` não lista plano inativo.
- [x] **C14** `contrato-api` — `GET /api/v1/plans` sem autenticação responde 200.
- [x] **C15** `contrato-api` — Plano com limite de anúncios nulo é devolvido como ilimitado.

## Contrato técnico
- Entidade `Plan` em tabela própria, com `nome` único, `descricao`, `precoMensal`, `limiteAnuncios` e `ativo`.
- **`limiteAnuncios` nulo significa ilimitado.** Um número mágico grande no plano avançado viraria limite de verdade no dia em que alguém o ultrapassasse.
- `precoMensal` é `BigDecimal`, como `Announcement.price`. Dinheiro não é `double`.
- Plano **não é removido**, apenas desativado: plano apagado deixa órfã a assinatura que aponta para ele, e a spec de pagamento vai apontar.
- O catálogo público é `GET /api/v1/plans`, liberado em `SecurityConfig`. Preço de pacote é informação de venda: exigir conta para ver quanto custa afasta quem ainda está decidindo.
- O painel interno usa `/api/v1/backoffice/plans`, já fechado por papel desde a spec 01. Nenhum método repete a checagem.
- Nada aqui liga plano a empresa. Assinatura, cobrança e o que o plano libera são da spec de pagamento.

## Fora de escopo
- Assinatura de plano por uma empresa, cobrança e gateway — spec de pagamento.
- Bloqueio de anúncio da PJ por limite do plano — depende de existir assinatura.
- Preço promocional, período de teste, desconto e ciclo anual.
- Telas do painel e da página de preços — spec de frontend própria.
- Histórico de alteração de preço: o plano guarda o preço vigente, não a série.

## Plano
1. `model/Plan.java` — entidade com nome único.
2. `repositories/PlanRepository.java` — `existsByNome`, `findAllByAtivoTrue`.
3. `dtos/PlanDTO.java`, `dtos/PlanCreateDTO.java`, `dtos/PlanUpdateDTO.java`.
4. `services/PlanService.java` — criação, alteração, desativação e os dois catálogos.
5. `controllers/PlanController.java` — `GET /api/v1/plans`.
6. `controllers/BackofficeController.java` — `GET`, `POST`, `PUT` e `POST /{id}/desativar`.
7. `config/SecurityConfig.java` — `GET /api/v1/plans` liberado.

Fatia única: os dois catálogos e as escritas dependem da mesma entidade.

## Perguntas em aberto
nenhuma
