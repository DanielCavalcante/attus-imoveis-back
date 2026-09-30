# 24-coerencia-do-anuncio-fora-do-ar — veredito

Medido contra `8a70b32..HEAD`, escopo `.`, com `CoerenciaDoAnuncioForaDoArSpecTest`
(10/10) e a suíte completa do backend (228 testes, 227 passam, 1 pulado).

- **C1** — atendido. Dois anúncios favoritados, um inativado: a lista devolve só o que continua no ar, e o JSONPath do que saiu vem vazio.
- **C2** — atendido. Favoritado e inativado, o total vai a zero; reativado, o mesmo anúncio volta à lista com o título certo. O favorito não foi apagado, só deixou de ser mostrado — é a diferença entre esconder e destruir dado de terceiro.
- **C3** — atendido. Com um dentro e um fora do ar, `totalElements` é 1. O `countQuery` filtra junto com a consulta.
- **C4** — atendido. Dois anúncios visitados, um inativado: o histórico não traz o que saiu do ar.
- **C5** — atendido. Visitado e inativado, o total vai a zero; reativado, volta a 1.
- **C6** — atendido. `totalElements` do histórico é 1 com um dentro e um fora do ar.
- **C7** — atendido. `POST /announcements/{id}/favorito` sobre anúncio inativado responde 404.
- **C8** — atendido. `PUT /announcements/{id}` com o token de outra conta responde 404, e o título do anúncio continua sendo o original quando lido depois. A segunda observação é o que prova que a recusa não foi parcial.
- **C9** — atendido. O status da edição de um anúncio que existe e não é seu é **igual** ao de um id que não existe (404 nos dois). Comparar as duas respostas não conta ao curioso qual id está ocupado.
- **C10** — atendido. O dono edita o próprio anúncio e o título muda para o novo.

## A regressão que a suíte completa pegou, e que eu causei

`EstadoDoAnuncioSpecTest.c8` reprovou na primeira execução da suíte inteira: **200 esperado, 404 recebido**, no trecho em que o **dono** abre o detalhe do próprio anúncio fora do ar.

Causa: eu pus o filtro de estado dentro de `requireAnnouncement`, que é **compartilhado** entre favoritar e registrar visita. A rota de detalhe chama `registerView` depois de servir o anúncio; com o filtro ali, o registro estourava 404 e derrubava a resposta inteira — inclusive para quem tinha todo o direito de ver.

Correção: o filtro virou `requireAnnouncementNoAr`, usado **só** pelo favorito. `registerView` voltou ao lookup permissivo.

Vale registrar o que isso mostra: o critério C8 da spec 22 existia justamente para garantir que o dono continua vendo o que é dele, e foi ele que pegou. Se a suíte completa não tivesse rodado, a spec 24 fecharia verde com 10/10 e teria quebrado a 22 em silêncio.

## Observações que não são critério

- **A alternativa não escolhida está registrada no contrato técnico:** mostrar o favorito marcado como "fora do ar" preservaria mais informação, mas exigiria o detalhe servir anúncio inativo a quem favoritou, reabrindo o C8 da spec 22.
- O `edit` agora usa o mesmo `doDono` que a spec 22 criou para inativar, reativar e excluir — não é código novo, é a rota que faltava passar a usá-lo.
- Nenhuma tela mudou. A página de gerenciamento e a de favoritos consomem essas rotas sem alteração.
