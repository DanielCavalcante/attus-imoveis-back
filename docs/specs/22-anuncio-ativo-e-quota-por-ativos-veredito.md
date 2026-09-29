# 22-anuncio-ativo-e-quota-por-ativos — veredito

Medido contra `1ceced2..HEAD`, escopo `.`, com `EstadoDoAnuncioSpecTest` (15/15)
e a suíte completa do backend (218 testes).

- **C1** — atendido. Anúncio recém-criado sai com `active = true`. A coluna nasce `NOT NULL DEFAULT TRUE` e o campo da entidade já inicializa em `true`, então o padrão não depende de quem gravou.
- **C2** — atendido. `PATCH /announcements/{id}/inativar` responde 204 e a entidade passa a `active = false`.
- **C3** — atendido. `PATCH /announcements/{id}/reativar` responde 204 sobre anúncio inativado e a entidade volta a `active = true`.
- **C4** — atendido. `DELETE /announcements/{id}` responde 204 e o anúncio some do repositório. Nenhum 500: favorito, histórico e pedido de apoio já têm cascata declarada na entidade.
- **C5** — atendido. Inativar com o token de outra conta responde 404 e o anúncio permanece ativo. 404 e não 403 de propósito: responder "existe, mas não é seu" conta ao curioso que aquele id existe.
- **C6** — atendido. Excluir com o token de outra conta responde 404 e o anúncio permanece no repositório.
- **C7** — atendido. Com um anúncio inativado e outro ativo, a listagem pública devolve `totalElements = 1` e traz só o ativo.
- **C8** — atendido. `GET /announcements/{id}` de anúncio inativado responde 404 sem sessão, e 200 com o token do dono. Duas observações na mesma execução, e a segunda é o que impede o critério de ser satisfeito por um 404 indiscriminado.
- **C9** — atendido. `GET /announcements/me` do dono devolve o anúncio inativado.
- **C10** — atendido. PF com dois anúncios criados no mesmo mês reporta `usados = 2`; depois de inativar um, reporta `usados = 1`. Criados no mês continuam sendo dois — é a contagem que mudou, não os dados.
- **C11** — atendido. Conta **PJ**: `usados = 1`, e depois de inativar, `usados = 0`. Antes desta spec a PJ nem tinha saldo — caía em `semLimite()` com todos os campos nulos.
- **C12** — atendido. Conta **PJ**: excluir leva `usados` de 1 para 0.
- **C13** — atendido. PF com três no ar, um inativado e a vaga ocupada por um quarto anúncio: reativar o antigo responde 409.
- **C14** — atendido. PF com três no ar recebe 409 ao criar o quarto.
- **C15** — atendido. Todos os anúncios do repositório saem da migração com `active = true`.

## O que a suíte completa expôs, e não é critério

- **Dois testes reprovaram pela substituição que a spec 22 declarou**, e os dois foram tratados, não silenciados:
  - `QuotaConfiguravelSpecTest` fixava o limite por `attus.quota.pf.mensal`, chave que deixou de existir. Renomeada para `attus.quota.pf.ativos`.
  - `QuotaMensalDaPfSpecTest.c6` afirmava que "anúncio de mês anterior não conta para a quota do mês". Com a contagem por ativos isso **deixou de ser verdade por decisão**: idade não abre vaga, inativar ou excluir abre. O teste foi reescrito para guardar a regra nova e o **C6 da spec 06 foi marcado como substituído** no próprio arquivo, com data. Manter os dois verdes exigiria manter as duas regras ao mesmo tempo, que é o que a spec 22 recusa.

- **Teste instável pré-existente, fora do escopo desta spec.** `FalhaDeTokenSpecTest.c4` falhou uma vez em três execuções, sem nenhuma alteração de código entre elas. Ele quebra a assinatura do JWT trocando o **último** caractere base64url; naquela posição parte dos bits não é significativa, então algumas trocas decodificam na mesma assinatura e o token continua válido. Não é regressão desta spec: o teste só exercita `/announcements/me` e a validação de token, que esta spec não tocou. Corrigir exige alterar caractere do miolo da assinatura, e é spec própria.

- **Achado de segurança que continua aberto:** `AnnouncementService.edit` recebe o e-mail da sessão e **não o usa** — qualquer conta autenticada edita anúncio de qualquer outra. Esta spec resolveu o mesmo padrão em inativar, reativar e excluir, e deixou `edit` como estava porque escopo estrito é regra do método. A rota **está no ar**.
