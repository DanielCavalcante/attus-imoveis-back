# 06-quota-mensal-da-pf

## Intenção
A pessoa física publica até três anúncios por mês sem pagar, e sabe quantos ainda tem — hoje qualquer conta publica quanto quiser.

## Depende de
01-contas-tipadas

## Critérios
- [ ] **C1** `dominio` — Conta `PF` sem anúncio no mês cria um anúncio com sucesso.
- [ ] **C2** `dominio` — Conta `PF` com dois anúncios no mês cria o terceiro com sucesso.
- [ ] **C3** `dominio` — Conta `PF` com três anúncios no mês é recusada ao criar o quarto.
- [ ] **C4** `contrato-api` — A recusa por quota responde 409.
- [ ] **C5** `persistencia` — Anúncio recusado por quota não é gravado.
- [ ] **C6** `dominio` — Anúncio criado em mês anterior não conta para a quota do mês corrente.
- [ ] **C7** `dominio` — Anúncio de outra conta não conta para a quota desta.
- [ ] **C8** `dominio` — Conta `PJ` com três anúncios no mês cria o quarto com sucesso.
- [ ] **C9** `dominio` — Com o limite configurado em 1, a conta `PF` é recusada ao criar o segundo anúncio.
- [ ] **C10** `contrato-api` — `GET /api/v1/announcements/me/quota` devolve o limite vigente.
- [ ] **C11** `contrato-api` — `GET /api/v1/announcements/me/quota` devolve quantos anúncios já foram usados no mês.
- [ ] **C12** `contrato-api` — `GET /api/v1/announcements/me/quota` devolve quantos ainda restam.
- [ ] **C13** `contrato-api` — `GET /api/v1/announcements/me/quota` para conta `PJ` indica que não há limite.
- [ ] **C14** `seguranca` — `GET /api/v1/announcements/me/quota` sem autenticação responde 403.

## Contrato técnico
- O limite vem da propriedade `attus.quota.pf.mensal`, com padrão 3. Literal no código transformaria mudança de regra de negócio em deploy de código.
- A quota incide sobre `AccountType.PF` apenas. `PJ` publica conforme plano, que é a spec 07; `INTERNO` não publica pela porta pública.
- A contagem usa `createdAt` do anúncio, do primeiro instante do mês corrente em diante. Mês civil, não janela de trinta dias: "três por mês" é o que foi combinado com quem usa.
- A contagem é consulta agregada no banco, não `findAll().size()`: a lista de anúncios de uma conta cresce e carregar tudo para contar é desperdício que só aparece tarde.
- Recusa por quota é 409, não 403: o pedido é legítimo e a conta tem permissão; o que falta é saldo. 403 diria que a conta não pode publicar, o que é falso.
- A verificação acontece em `AnnouncementService.create`, antes de qualquer gravação, para que a recusa não deixe rastro.

## Fora de escopo
- Plano da pessoa jurídica e qualquer limite associado — spec 07.
- Compra de anúncio avulso ou upgrade de quota pela pessoa física.
- Tela que mostra a quota — spec de frontend própria; aqui só a API.
- Quota por outro recurso que não anúncio (foto, destaque, contato).
- Devolver saldo quando um anúncio é removido: `DELETE` de anúncio não existe na API hoje.

## Plano
1. `repositories/AnnouncementRepository.java` — `countByUserEmailAndCreatedAtGreaterThanEqual`.
2. `dtos/AnnouncementQuotaDTO.java` — limite, usados, restantes e o indicador de ilimitado.
3. `services/AnnouncementService.java` — checagem antes de gravar e cálculo da quota.
4. `controllers/AnnouncementController.java` — `GET /me/quota`.
5. `src/main/resources/application.properties` — `attus.quota.pf.mensal=3`.

Fatia única: o endpoint de consulta e a recusa usam o mesmo cálculo.

## Perguntas em aberto
nenhuma
