# Veredito — 07-planos-configuraveis

Base: estado da árvore após a spec 06, mais os arquivos novos de `.` (nada commitado desde `1e03b12`).

> **Limitação de independência.** Produzido pela mesma sessão que escreveu o código, a pedido do humano. O passo 2 do `verificar` pede contexto limpo porque quem escreve o código escreve os testes. Leia sabendo que essa garantia não existe aqui.

- **C1** — atendido. `/api/v1/backoffice/**` exige `hasRole("INTERNO")` desde a spec 01; token de conta comum é barrado no filtro, antes do controller.

- **C2** — atendido. `createPlan` devolve `HttpStatus.CREATED` explicitamente, em vez do 200 padrão do `ResponseEntity.ok`.

- **C3** — atendido. O teste relê a entidade do banco e compara nome, preço e limite; o preço é comparado com `isEqualByComparingTo`, que não reprova por escala de `BigDecimal`.

- **C4** — atendido. `@NotBlank` em `PlanCreateDTO.nome` mais o handler de `MethodArgumentNotValidException`, que prefixa a mensagem com o nome do campo.

- **C5** — atendido. `@PositiveOrZero` em `precoMensal` recusa `-1.00`. Zero continua aceito de propósito: plano gratuito é decisão de negócio possível.

- **C6** — atendido. `existsByNome` antes da gravação lança `ConflictException` → 409, e a coluna também é única no banco.

- **C7** — atendido. `create` grava `ativo = true` explicitamente, sem depender do valor padrão do campo.

- **C8** — atendido. `update` grava o novo preço e o teste relê do banco.

- **C9** — atendido. Mesmo fechamento por papel de C1, agora no método de alteração.

- **C10** — atendido. `deactivate` grava `ativo = false`.

- **C11** — atendido. `deactivate` chama `save`, nunca `delete`; o teste confirma que `findById` ainda encontra o plano depois de desativado.

- **C12** — atendido. O catálogo interno usa `findAll`, e o teste confirma `ativo: false` na listagem — reprovaria se o painel usasse o mesmo filtro do catálogo público.

- **C13** — atendido. O catálogo público usa `findAllByAtivoTrue`, e a lista volta vazia quando o único plano está desativado.

- **C14** — atendido. `GET /api/v1/plans` foi liberado em `SecurityConfig` e responde 200 sem cabeçalho de autorização.

- **C15** — atendido. `PlanDTO.fromEntity` deriva `anunciosIlimitados` de `limiteAnuncios == null`, então o consumidor não precisa interpretar o nulo.

## Observações que não são critério

- Não existe rota para reativar um plano desativado. O painel lista os inativos (C12), mas só consegue alterá-los pelo `PUT`, que não mexe em `ativo`. Reativar exige mudança no banco.
- `PlanCreateDTO` e `PlanUpdateDTO` são idênticos campo a campo. Enquanto permanecerem assim, são dois lugares para manter a mesma validação.
- O preço é guardado sem moeda. Enquanto o produto for só Brasil, `BigDecimal` basta; para outro país, o valor não diz em que moeda está.
- Alterar o preço de um plano muda o preço para todos, inclusive quem já contratou. Quando a assinatura existir, isso precisa de decisão: congelar o preço na contratação ou reajustar todo mundo.
