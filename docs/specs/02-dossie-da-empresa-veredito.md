# Veredito — 02-dossie-da-empresa

Base: estado da árvore após a spec 01, mais os arquivos novos de `.` (nada commitado desde `1e03b12`).

> **Limitação de independência.** Produzido pela mesma sessão que escreveu o código, a pedido do humano. O passo 2 do `verificar` pede contexto limpo porque quem escreve o código escreve os testes. Leia sabendo que essa garantia não existe aqui.

- **C1** — atendido. `UserService.create` não consulta nenhum campo de dossiê; a criação da `Company` recebe só o CNPJ já validado pela spec 01, então o cadastro PJ mínimo responde 201.

- **C2** — atendido. `UserService.create` chama `companyService.createFor(saved, saved.getCnpj())` no ramo `accountType == PJ`, e o teste relê por `findByUserId` em vez de confiar no retorno do serviço.

- **C3** — atendido. A criação da empresa está dentro do `if (saved.getAccountType() == AccountType.PJ)`; o teste confirma ausência pelo mesmo `findByUserId`.

- **C4** — atendido. `CompanyService.createFor` grava `dossieCompleto = false` explicitamente, e o campo tem `nullable = false` com default `false` na entidade.

- **C5** — atendido. `GET /api/v1/companies/me` resolve a empresa por `userDetails.getUsername()`, que é o e-mail do token, e o teste casa o CNPJ do corpo com o que foi enviado no cadastro.

- **C6** — atendido. A rota cai em `anyRequest().authenticated()`; sem cabeçalho, o filtro não popula o contexto e a resposta é 403.

- **C7** — atendido. Conta PF não tem empresa, `requireCompany` lança `ResourceNotFoundException`, e o handler dessa exceção passou a responder 404 — antes respondia 400 apesar do nome.

- **C8** — atendido. `PUT /api/v1/companies/me` com o corpo completo passa nas validações do `CompanyUpdateDTO` e retorna 200.

- **C9** — atendido. `CompanyService.apply` copia os catorze campos, e o teste relê do banco conferindo razão social, nome fantasia, CRECI, responsável e cidade — não o corpo da resposta.

- **C10** — atendido. `isComplete` exige os treze campos obrigatórios (complemento fora), e o resultado é gravado em `dossieCompleto` antes do `save`.

- **C11** — atendido. `@NotBlank` em `razaoSocial` mais o handler de `MethodArgumentNotValidException` da spec 01, que prefixa a mensagem com o nome do campo.

- **C12** — atendido. Mesmo mecanismo em `creciNumero`.

- **C13** — atendido. `validateCreci` consulta `existsByCreciNumeroAndCreciUf` e lança `ConflictException` → 409. A guarda de "mesmo CRECI que já era meu" evita que a empresa colida consigo mesma ao reenviar o dossiê.

- **C14** — atendido. Não existe rota por id: o serviço só resolve a empresa pelo e-mail do token. O teste grava o dossiê de uma conta e verifica que a razão social da outra continua nula.

- **C15** — atendido. `cnpj` é `varchar` na `Company` e em `User`, sem conversão numérica em nenhum ponto do caminho; o teste grava `AB<n>CD0001X` e relê idêntico.

## Observações que não são critério

- A `Company` nasce com o CNPJ copiado de `User`, então o dado existe em dois lugares. Enquanto o CNPJ não puder ser editado, os dois não divergem; quando a spec 05 permitir correção de CNPJ reprovado, um dos dois tem de deixar de ser fonte.
- `GET` e `PUT` em `/companies/me` estão abertos a qualquer conta autenticada, inclusive `INTERNO`. Uma conta interna não tem empresa, então recebe 404 — o efeito é correto, mas por ausência de dado, não por regra.
