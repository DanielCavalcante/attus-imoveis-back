# 45-a-conta-so-mexe-em-si-mesma

## Intenção
Uma conta passa a alcançar apenas a si mesma: não lista, não lê, não altera e
não apaga nenhuma outra.

## Depende de
nenhuma

## Critérios

- [ ] **C1** `seguranca` — editar outra conta é recusado, e o dado dela continua como estava.
- [ ] **C2** `seguranca` — excluir outra conta é recusado, e a conta continua existindo.
- [ ] **C3** `seguranca` — ler o detalhe de outra conta é recusado.
- [ ] **C4** `seguranca` — a rota que lista contas não é alcançável por conta comum.
- [ ] **C5** `dominio` — editar a própria conta continua funcionando.
- [ ] **C6** `dominio` — excluir a própria conta continua funcionando.
- [ ] **C7** `dominio` — ler o próprio detalhe continua funcionando.
- [ ] **C8** `seguranca` — a recusa não revela se a outra conta existe.
- [ ] **C9** `seguranca` — passar o id de outra conta no corpo não muda quem é alterado: quem manda é a sessão.
- [ ] **C10** `seguranca` — a conta interna continua alcançando o que o backoffice precisa.
- [ ] **C11** `contrato-api` — corpo inválido responde 400, e não 403.

## Contrato técnico

**Medido antes de escrever esta spec, contra a API rodando:**

- `GET /users` com token de uma conta comum devolveu **os 14 usuários**, com
  e-mail de cada um;
- `GET /users/15` devolveu o detalhe de outra pessoa;
- `PUT /users/16` com token da conta 14 respondeu **200** e gravou
  "Carol INVADIDA pela Alice" no nome dela;
- `DELETE /users/15` com token da conta 14 respondeu **204**, e a conta sumiu
  do banco.

Nenhum dos quatro métodos de `UserController` olha quem está chamando:
`edit(id, dto)` e `delete(id)` recebem o id do caminho e nunca o comparam com
a sessão. É a mesma forma do buraco que a spec 22 fechou nos anúncios, e o
comentário que ela deixou em `AnnouncementService.edit` descreve este caso
palavra por palavra -- "qualquer conta autenticada editava anuncio de qualquer
outra, com a rota no ar".

**A identidade vem da sessão, não do caminho.** É o que C9 fixa: enquanto o id
vier de fora, cada rota nova precisa lembrar de conferir, e uma vai esquecer.
A correção não é conferir melhor o `{id}` -- é parar de aceitá-lo como
identidade.

**C8 pelo mesmo motivo do 404 de `doDono`**, na spec 22: responder "existe, mas
não é seu" conta ao curioso que aquele id existe, e com ids sequenciais isso
enumera a base inteira.

**C11 saiu da própria medição.** `PUT` com corpo sem `email` respondeu 403, e
não 400 -- a mensagem manda quem integra procurar problema de permissão onde
há campo faltando. Entrou como critério porque foi encontrado ao provar os
outros.

## Fora de escopo

- **Trocar o `{id}` da rota por `/me`.** Seria contrato novo, e as telas do
  front teriam de acompanhar na mesma tarefa. C9 garante o que importa agora:
  o id do caminho deixa de decidir quem é alterado.
- **Quem pode ver o quê no backoffice.** A conta interna continua como está
  (C10); rever os poderes dela é decisão própria.
- **Apagar em cascata o que a conta deixou para trás** ao excluir a si mesma.
  É o comportamento de hoje, e mudá-lo é outra decisão.
- **O front.** Nenhuma tela hoje lista ou edita conta alheia, então não há o
  que corrigir lá -- o buraco era a API aceitar, não a tela oferecer.

## Plano

1. A identidade passa a vir da sessão — `controllers/UserController.java`,
   `services/UserService.java`.
2. A listagem sai do alcance de conta comum — `config/SecurityConfig.java`.
3. Recusa que não revela existência — `services/UserService.java`.
4. Testes por critério —
   `src/test/java/com/example/demo/ContaSoMexeEmSiSpecTest.java`.

## Perguntas em aberto

Nenhuma.
