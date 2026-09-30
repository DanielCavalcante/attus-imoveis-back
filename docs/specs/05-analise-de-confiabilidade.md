# 05-analise-de-confiabilidade

## Intenção
A imobiliária envia o dossiê para análise e a equipe interna aprova ou reprova com motivo — hoje qualquer empresa com CNPJ entra na plataforma sem ninguém olhar.

## Depende de
02-dossie-da-empresa

> **Aprovada no gate humano em 2026-09-28.** Todos os critérios aceitos.

## Critérios
- [x] **C1** `dominio` — Empresa recém-criada tem análise no estado `NAO_ENVIADO`.
- [x] **C2** `dominio` — Envio para análise de empresa com dossiê incompleto é recusado com 400.
- [x] **C3** `dominio` — Envio para análise de empresa com dossiê completo deixa a análise em `EM_ANALISE`.
- [x] **C4** `seguranca` — `POST /api/v1/companies/me/analise` sem autenticação responde 403.
- [x] **C5** `contrato-api` — `GET /api/v1/companies/me` devolve o estado da análise.
- [x] **C6** `seguranca` — `GET /api/v1/backoffice/companies` com papel diferente de `INTERNO` responde 403.
- [x] **C7** `contrato-api` — `GET /api/v1/backoffice/companies` lista empresa em `EM_ANALISE`.
- [x] **C8** `contrato-api` — `GET /api/v1/backoffice/companies` não lista empresa em `NAO_ENVIADO`.
- [x] **C9** `dominio` — Aprovação por conta interna deixa a análise em `APROVADA`.
- [x] **C10** `dominio` — Reprovação por conta interna deixa a análise em `REPROVADA`.
- [x] **C11** `contrato-api` — Reprovação sem motivo é recusada com 400 e a mensagem nomeia o campo.
- [x] **C12** `persistencia` — O motivo informado na reprovação fica gravado na empresa.
- [x] **C13** `persistencia` — A aprovação grava o e-mail de quem aprovou e o instante da decisão.
- [x] **C14** `seguranca` — Aprovar com papel diferente de `INTERNO` responde 403.
- [x] **C15** `dominio` — Empresa já `APROVADA` não pode ser reenviada para análise.

## Contrato técnico
- `enums/AnalysisStatus`: `NAO_ENVIADO`, `EM_ANALISE`, `APROVADA`, `REPROVADA`. Persistido como `EnumType.STRING`.
- Campos novos em `Company`: `analysisStatus`, `rejectionReason`, `reviewedBy`, `reviewedAt`. Os três últimos são nulos até a primeira decisão.
- `reviewedBy` guarda o **e-mail** de quem decidiu, não o id: a trilha precisa continuar legível se a conta interna for removida.
- Empresa `REPROVADA` **pode** ser reenviada: reprovar por dado errado e trancar para sempre transformaria erro de digitação em perda de cliente. `APROVADA` não pode, porque reanálise de empresa já aprovada é outra demanda.
- Salvar o dossiê não reseta a análise. Se a spec de pagamento precisar que alteração pós-aprovação volte à fila, isso é critério dela, não desta.
- A transição de estado mora em `CompanyService`, não no controller. O backoffice e a empresa chamam o mesmo serviço por portas diferentes.
- As rotas de backoffice já nascem fechadas por papel: a spec 01 registrou `/api/v1/backoffice/**` com `hasRole("INTERNO")` em `SecurityConfig`. Esta spec não mexe em `SecurityConfig`.
- Aprovação não libera nada por si: quem decide o que a PJ aprovada pode fazer é a spec de plano e pagamento.

## Fora de escopo
- Consulta automática a Receita Federal ou conselho regional — a decisão aqui é humana; automatizar a apuração é spec própria.
- Notificação da decisão para a empresa — depende do serviço de e-mail, adiado pelo gate.
- Telas do backoffice e da empresa — spec de frontend própria; aqui só a API.
- Plano, pagamento e qualquer bloqueio de anúncio por estado da análise.
- Histórico de decisões: a empresa guarda a decisão vigente, não a série. Auditoria completa é outra demanda.

## Plano
1. `enums/AnalysisStatus.java`.
2. `model/Company.java` — `analysisStatus` com padrão `NAO_ENVIADO`, mais `rejectionReason`, `reviewedBy`, `reviewedAt`.
3. `repositories/CompanyRepository.java` — `findAllByAnalysisStatus`.
4. `dtos/CompanyDetailDTO.java` — expõe o estado da análise e o motivo.
5. `dtos/BackofficeCompanyListDTO.java` e `dtos/CompanyRejectionDTO.java`.
6. `services/CompanyService.java` — `submitForAnalysis`, `approve`, `reject`.
7. `controllers/CompanyController.java` — `POST /me/analise`.
8. `controllers/BackofficeController.java` — `GET /companies`, `POST /companies/{id}/aprovar`, `POST /companies/{id}/reprovar`.

Fatia única: os critérios de `seguranca` e `contrato-api` dependem do estado existir na entidade.

## Perguntas em aberto
nenhuma
