# Veredito — 05-analise-de-confiabilidade

Base: estado da árvore após a spec 02, mais os arquivos novos de `.` (nada commitado desde `1e03b12`).

> **Limitação de independência.** Produzido pela mesma sessão que escreveu o código, a pedido do humano. O passo 2 do `verificar` pede contexto limpo porque quem escreve o código escreve os testes. Leia sabendo que essa garantia não existe aqui.

- **C1** — atendido. `Company.analysisStatus` é inicializado em `NAO_ENVIADO` na declaração do campo, então a empresa criada junto da conta PJ já nasce nesse estado sem depender de o serviço atribuí-lo.

- **C2** — atendido. `submitForAnalysis` consulta `isDossieCompleto` antes de qualquer transição e lança `BusinessException`, que responde 400 desde a spec 01.

- **C3** — atendido. Com o dossiê completo, o serviço grava `EM_ANALISE` e o teste relê do banco em vez de conferir o corpo da resposta.

- **C4** — atendido. `/api/v1/companies/**` cai em `anyRequest().authenticated()`; sem cabeçalho de autorização a resposta é 403.

- **C5** — atendido. `CompanyDetailDTO` ganhou `analysisStatus` e `rejectionReason`, e o teste casa o valor no JSON de `GET /companies/me`.

- **C6** — atendido. `/api/v1/backoffice/**` exige `hasRole("INTERNO")` desde a spec 01; token de conta PJ com papel `COMUM` recebe 403 sem chegar ao controller.

- **C7** — atendido. `findAllByAnalysisStatus(EM_ANALISE)` alimenta a fila, e o teste confirma o estado no primeiro item da lista.

- **C8** — atendido. Empresa com dossiê completo mas sem envio permanece `NAO_ENVIADO` e a fila volta vazia — provado com `jsonPath("$").isEmpty()`, que reprovaria se a consulta trouxesse tudo.

- **C9** — atendido. `approve` grava `APROVADA` e limpa `rejectionReason`, para que motivo de uma reprovação anterior não fique pendurado numa empresa aprovada.

- **C10** — atendido. `reject` grava `REPROVADA`.

- **C11** — atendido. `@NotBlank` em `CompanyRejectionDTO.motivo` mais o handler de `MethodArgumentNotValidException`, que prefixa a mensagem com o nome do campo. O teste envia string vazia, não campo ausente — `@NotBlank` cobre os dois.

- **C12** — atendido. O motivo é gravado em `rejectionReason` e o teste relê do banco.

- **C13** — atendido. `registerDecision` grava `reviewedBy` com o e-mail vindo de `UserDetails` do token e `reviewedAt` com o instante da decisão. O teste cria a conta interna, guarda o e-mail e compara.

- **C14** — atendido. Mesmo fechamento por papel de C6, agora no método de aprovação.

- **C15** — atendido. `submitForAnalysis` recusa quando o estado é `APROVADA`, com 400. A recusa é só para aprovada: `REPROVADA` volta à fila de propósito, conforme o contrato técnico.

## Observações que não são critério

- Aprovar ou reprovar não valida o estado de origem: o backoffice consegue aprovar empresa `NAO_ENVIADO` chamando a rota diretamente com o id. A fila só mostra `EM_ANALISE`, então pela interface isso não acontece — mas a API aceita.
- `findPendingAnalysis` devolve a fila inteira, sem paginação nem ordenação. Com volume, a rota fica cara e a ordem de atendimento fica indefinida.
- Salvar o dossiê depois da aprovação não devolve a empresa para a fila. Está declarado no contrato técnico como decisão da spec de pagamento, mas enquanto ninguém a tomar, uma empresa aprovada pode trocar CRECI e razão social sem nova análise.
