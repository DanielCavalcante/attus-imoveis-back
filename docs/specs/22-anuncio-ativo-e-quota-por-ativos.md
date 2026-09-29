# 22-anuncio-ativo-e-quota-por-ativos

## Intenção
Quem anuncia passa a tirar o imóvel do ar sem perdê-lo, e a quota deixa de contar quantos anúncios criou no mês para contar quantos mantém no ar.

## Depende de
06-quota-mensal-da-pf

> **Aprovada no gate humano em 2026-09-29.** Todos os critérios aceitos.

## Critérios
- [ ] **C1** `dominio` — Anúncio recém-criado nasce ativo.
- [ ] **C2** `contrato-api` — Existe rota para inativar um anúncio da própria conta.
- [ ] **C3** `contrato-api` — Existe rota para reativar um anúncio da própria conta.
- [ ] **C4** `contrato-api` — Existe rota para excluir um anúncio da própria conta.
- [ ] **C5** `seguranca` — Inativar anúncio de outra conta é recusado.
- [ ] **C6** `seguranca` — Excluir anúncio de outra conta é recusado.
- [ ] **C7** `dominio` — Anúncio inativo não aparece na listagem pública.
- [ ] **C8** `dominio` — Detalhe de anúncio inativo não é servido a quem não é o dono.
- [ ] **C9** `dominio` — Anúncio inativo continua aparecendo na listagem da própria conta.
- [ ] **C10** `dominio` — O saldo de anúncios conta anúncios ativos, não anúncios criados no mês.
- [ ] **C11** `dominio` — Inativar um anúncio devolve uma vaga ao saldo, qualquer que seja o tipo de conta.
- [ ] **C12** `dominio` — Excluir um anúncio devolve uma vaga ao saldo, qualquer que seja o tipo de conta.
- [ ] **C13** `dominio` — Reativar com a quota cheia é recusado com 409.
- [ ] **C14** `dominio` — Criar com a quota cheia continua recusado com 409.
- [ ] **C15** `persistencia` — Os anúncios já publicados ficam ativos depois da migração.

## Contrato técnico
- **Esta spec substitui a regra de contagem da spec 06, e isso é mudança de produto, não refactor.** Hoje a pessoa física cria três por mês e acumula: em um ano pode ter trinta e seis no ar. Passando a contar ativos, ela mantém três no ar e troca quantas vezes quiser. Mais restritivo no acúmulo, mais generoso no giro. A chave `attus.quota.pf.mensal` deixa de descrever o que faz e vira `attus.quota.pf.ativos`.
- **Contar é de todo mundo; limitar é que varia.** Hoje `hasQuota` devolve `false` para PJ e a contagem nem acontece: a conta jurídica não tem saldo nenhum, tem ausência de assunto. A contagem de ativos passa a valer para qualquer tipo de conta, e o que depende do tipo é só o limite — configuração na pessoa física, plano na jurídica quando o vínculo existir. É o que faz C11 e C12 valerem antes de a PJ ter limite: no dia em que tiver, devolver a vaga já funciona, em vez de ser mais uma coisa a lembrar de ligar.
- **Conta sem limite tem saldo, não tem teto.** A rota de saldo responde para a PJ com quantos anúncios ela mantém no ar; o que ela não traz é um restante inventado.
- **Inativar não é excluir, e o produto precisa dos dois.** Inativar tira do ar e devolve a vaga, preservando o anúncio para voltar depois — é o caso de quem vendeu e pode voltar a vender. Excluir é definitivo.
- **Excluir não pode quebrar no que pende do anúncio.** Favorito, histórico de visita e pedido de apoio à venda apontam para ele. Este repositório já produziu 500 em `DELETE` duas vezes, por FK sem cascata em `companies` e em `support_requests`; a exclusão fecha com o comportamento declarado no banco, não confiando na ordem em que o JPA resolve.
- **A rota de exclusão nasce com dono verificado.** Existe hoje um `@DeleteMapping` comentado no controller chamando `service.delete(id)` sem o e-mail da sessão: descomentado, seria qualquer conta apagando anúncio de qualquer outra. C6 é o critério que impede isso de renascer assim.
- Anúncio inativo some do acervo público, mas o dono continua vendo o dele. Some também do detalhe: link antigo de anúncio fora do ar não pode continuar servindo a página como se nada tivesse mudado.
- Migração `V6`, sob a disciplina da spec 12: coluna de estado com padrão ativo, e a carga deixa ativo o que já está publicado.

## Fora de escopo
- **O limite da pessoa jurídica pelo plano.** `Plan` tem `limiteAnuncios`, mas nada liga `Company` a um plano: o campo não existe. Criar esse vínculo é demanda própria e ela precisa antes decidir quem atribui o plano a uma empresa, já que não há pagamento no sistema para disparar isso. Até lá a PJ conta os ativos dela e não tem teto — a contagem entra agora justamente para o teto ser a única coisa que falta depois.
- Expiração automática de anúncio por tempo no ar.
- Recuperar anúncio excluído: exclusão é definitiva, e prometer lixeira sem construí-la é pior que não ter.
- Histórico de quem inativou e quando.
- **A tela.** A página de gerenciamento é a spec seguinte, no repositório do front.

## Plano
1. `src/main/resources/db/migration/V6__estado_do_anuncio.sql` — coluna de estado com padrão ativo.
2. `model/Announcement.java` — o estado e o padrão na criação.
3. `repositories/AnnouncementRepository.java` — acervo público só com ativos; contagem de ativos por conta.
4. `services/AnnouncementService.java` — inativar, reativar, excluir com dono verificado; a contagem de ativos sai de dentro do ramo da pessoa física e o tipo de conta passa a decidir só o limite.
5. `controllers/AnnouncementController.java` — as três rotas, incluindo a exclusão que hoje está comentada.
6. `application.properties` — `attus.quota.pf.ativos`.
7. `src/test/java/com/example/demo/EstadoDoAnuncioSpecTest.java`.

Fatia única: C11 e C12 só se provam com o estado e a contagem no lugar ao mesmo tempo.

## Perguntas em aberto
nenhuma
