# 02-dossie-da-empresa

## Intenção
A pessoa jurídica cria a conta com CNPJ e contato, e completa os dados da empresa depois, em vez de abandonar um formulário longo na primeira tela.

## Depende de
01-contas-tipadas

## Critérios
- [ ] **C1** `dominio` — Cadastro PJ com CNPJ e sem os demais dados da empresa é aceito com 201.
- [ ] **C2** `persistencia` — Conta criada com tipo `PJ` passa a ter um registro de empresa associado.
- [ ] **C3** `dominio` — Conta criada com tipo `PF` não gera registro de empresa.
- [ ] **C4** `dominio` — Empresa recém-criada tem o dossiê marcado como incompleto.
- [ ] **C5** `contrato-api` — `GET /api/v1/companies/me` com JWT de conta PJ devolve 200 com o CNPJ da conta.
- [ ] **C6** `seguranca` — `GET /api/v1/companies/me` sem cabeçalho de autorização responde 403.
- [ ] **C7** `contrato-api` — `GET /api/v1/companies/me` com JWT de conta PF responde 404.
- [ ] **C8** `contrato-api` — `PUT /api/v1/companies/me` com dossiê completo responde 200.
- [ ] **C9** `persistencia` — Após o `PUT` com dossiê completo, razão social, nome fantasia, CRECI, responsável e endereço estão gravados.
- [ ] **C10** `dominio` — Após o `PUT` com dossiê completo, a empresa fica marcada como completa.
- [ ] **C11** `contrato-api` — `PUT /api/v1/companies/me` sem razão social é recusado com 400 e a mensagem nomeia o campo.
- [ ] **C12** `contrato-api` — `PUT /api/v1/companies/me` sem número de CRECI é recusado com 400 e a mensagem nomeia o campo.
- [ ] **C13** `persistencia` — `PUT` com par CRECI número+UF já usado por outra empresa é recusado com 409.
- [ ] **C14** `seguranca` — `PUT /api/v1/companies/me` altera apenas a empresa da conta autenticada, deixando a de outra conta intacta.
- [ ] **C15** `persistencia` — CNPJ com letras é gravado e relido sem perda de caractere.

## Contrato técnico
- Entidade `Company` em tabela própria, `@OneToOne` com `User`. O CNPJ continua em `User`, onde a spec 01 o tornou único: ele identifica a conta no cadastro, antes de existir dossiê.
- `Company` guarda: `razaoSocial`, `nomeFantasia`, `creciNumero`, `creciUf`, `responsavelNome`, `responsavelCpf`, `telefoneComercial`, `emailComercial`, `cep`, `logradouro`, `numero`, `complemento`, `cidade`, `uf`, e o marcador `dossieCompleto`.
- `dossieCompleto` é campo gravado, não cálculo em tempo de leitura: a spec 05 vai consultar esse estado em fila, e recalcular por linha em consulta de lista não escala.
- Obrigatórios para o dossiê ficar completo: razão social, nome fantasia, CRECI número e UF, responsável nome e CPF, telefone comercial, e-mail comercial, CEP, logradouro, número, cidade, UF. `complemento` é opcional.
- **CNPJ e CRECI são texto, nunca número.** A Receita tem mudança de CNPJ para formato alfanumérico em curso; validar assumindo 14 dígitos numéricos cria trabalho de migração. Esta spec não valida dígito verificador de nenhum dos dois — ver Fora de escopo.
- O par `creciNumero` + `creciUf` é único no banco. Duas empresas com o mesmo CRECI significam que uma delas está mentindo, e é isso que a spec 05 vai apurar.
- Não existe rota de empresa por id: a conta só alcança a própria empresa, por `/me`. Isso elimina a classe inteira de referência direta insegura a objeto.

## Fora de escopo
- Envio do dossiê para análise e a fila do backoffice — spec 05. Aqui o dossiê só fica completo ou incompleto.
- Validação de dígito verificador de CNPJ, CPF e CRECI, e consulta a Receita ou a conselho regional — a apuração é da spec 05, com fonte externa.
- Inscrição estadual, porte e capital social — não entram na decisão de confiabilidade, e campo que ninguém usa é campo que alguém preenche errado.
- Tela de preenchimento do dossiê — spec 03, no frontend.
- Bloqueio de anúncio para PJ com dossiê incompleto — depende de plano e pagamento, specs 06 e 07.

## Plano
1. `model/Company.java` — entidade, `@OneToOne` com `User`, restrição única no par CRECI.
2. `repositories/CompanyRepository.java` — `findByUserId`, `existsByCreciNumeroAndCreciUf`.
3. `services/UserService.java` — criar `Company` com o CNPJ quando o tipo for `PJ`, e só então.
4. `dtos/CompanyDetailDTO.java` e `dtos/CompanyUpdateDTO.java` — o de entrada com as validações de obrigatoriedade.
5. `services/CompanyService.java` — leitura por conta autenticada, gravação do dossiê, cálculo e gravação de `dossieCompleto`, conflito de CRECI.
6. `controllers/CompanyController.java` — `GET` e `PUT` em `/api/v1/companies/me`.
7. `config/SecurityConfig.java` — `/api/v1/companies/**` exige autenticação, que já é o padrão; confirmar que nenhuma regra anterior a libera.
8. `exceptions/handler/GlobalExceptionHandler.java` — nada novo: 409 e 400 já existem desde a spec 01.

Fatia única: os critérios de `seguranca` e `contrato-api` dependem da entidade e do serviço existirem.

## Perguntas em aberto
nenhuma
