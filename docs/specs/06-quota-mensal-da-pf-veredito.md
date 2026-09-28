# Veredito — 06-quota-mensal-da-pf

Base: estado da árvore após a spec 05, mais os arquivos novos de `.` (nada commitado desde `1e03b12`).

> **Limitação de independência.** Produzido pela mesma sessão que escreveu o código, a pedido do humano. O passo 2 do `verificar` pede contexto limpo porque quem escreve o código escreve os testes. Leia sabendo que essa garantia não existe aqui.

- **C1** — atendido. `ensureQuota` conta zero anúncios no mês e libera a criação; o teste passa pelo endpoint real, não pelo serviço.

- **C2** — atendido. O terceiro anúncio passa porque a comparação é `usados >= limite`, não `>`: com dois gravados, o terceiro ainda cabe.

- **C3** — atendido. Com três gravados, `usados >= 3` recusa o quarto.

- **C4** — atendido. A recusa lança `ConflictException`, cujo handler responde 409 desde a spec 01. O critério é separado de C3 de propósito: um testa que recusa, o outro com que status.

- **C5** — atendido. `ensureQuota` roda antes de `mapper.toEntity` e de `repository.save`, então a recusa não grava nada. O teste conta três anúncios no banco depois da quarta tentativa.

- **C6** — atendido. A contagem parte do primeiro instante do mês corrente, calculado por `LocalDate.now().withDayOfMonth(1).atStartOfDay()`. O teste empurra os anúncios 40 dias para trás com SQL e cria o quarto com sucesso — sem depender de esperar a virada do mês.

- **C7** — atendido. `countByUserEmailAndCreatedAtGreaterThanEqual` filtra por e-mail da conta; três anúncios de outra conta não bloqueiam a primeira publicação desta.

- **C8** — atendido. `hasQuota` só responde verdadeiro para `AccountType.PF`, então a conta PJ atravessa `ensureQuota` sem checagem e publica o quarto.

- **C9** — atendido. Provado em classe própria com `@TestPropertySource(attus.quota.pf.mensal=1)`, que exercita a ligação da propriedade — não o campo. Com o limite em 1, o segundo anúncio recebe 409.

- **C10** — atendido. `AnnouncementQuotaDTO.de` devolve o limite vindo da propriedade, e o teste casa o valor 3 no JSON.

- **C11** — atendido. Com dois anúncios criados, `usados` vale 2.

- **C12** — atendido. `restantes` usa `Math.max(0, limite - usados)`, então nunca fica negativo se o limite for reduzido com anúncios já publicados.

- **C13** — atendido. Conta PJ recebe `AnnouncementQuotaDTO.semLimite()`, com `ilimitado = true` e os numéricos nulos, em vez de um número inventado.

- **C14** — atendido. `/api/v1/announcements/me/quota` tem dois segmentos após o prefixo, então não casa com o `permitAll` de `/api/v1/announcements/*`, que é de um segmento só. Cai em `anyRequest().authenticated()` e responde 403.

## Observações que não são critério

- **`GET /api/v1/announcements/me` é público.** A regra `permitAll` de `/api/v1/announcements/*` casa com `/me`, e o método usa `@AuthenticationPrincipal UserDetails`, que chega nulo sem token e estoura `NullPointerException` — 500 em vez de 403. É anterior a esta spec e nenhum critério daqui o toca, mas está aberto.
- A quota não é devolvida quando um anúncio é removido, porque a API não expõe remoção de anúncio. Quando expuser, a contagem por `createdAt` passa a permitir publicar, apagar e republicar sem fim dentro do mês.
- `LocalDate.now()` usa o fuso do servidor. Conta publicando perto da virada do mês pode ver o saldo mudar em hora diferente da que espera, se o servidor não estiver em horário de Brasília.
