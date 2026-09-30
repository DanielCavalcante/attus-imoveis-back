# Veredito — 11-correcoes-de-seguranca

Base: estado da árvore após a homologação do ciclo 01 (nada commitado desde `1e03b12`).

> **Limitação de independência.** Produzido pela mesma sessão que escreveu o código, a pedido do humano. O passo 2 do `verificar` pede contexto limpo porque quem escreve o código escreve os testes. Leia sabendo que essa garantia não existe aqui.

Além da suíte, os dois defeitos foram provados contra a stack em Docker, porque ambos foram descobertos por leitura e não por teste.

- **C1** — atendido. A regra de `/api/v1/announcements/me` e `/me/**` como autenticadas entra **antes** do `permitAll` de `/api/v1/announcements/*`; em `authorizeHttpRequests` o primeiro casamento vence. Contra a stack: a rota respondia 500 (o método chamava `getUsername()` num principal nulo) e passou a responder 403.

- **C2** — atendido. Com sessão a rota continua 200: a regra nova exige autenticação, não papel.

- **C3** — atendido. `GET /api/v1/announcements` sem token segue 200, confirmado na stack.

- **C4** — atendido. `GET /api/v1/announcements/{id}` sem token segue 200: o curinga de um segmento continua liberado para id numérico, e só `/me` foi retirado dele.

- **C5** — atendido. `/me/quota` já era protegida por ter dois segmentos; agora também casa com a regra explícita de `/me/**`, e o resultado não mudou.

- **C6** — atendido. Token emitido enquanto a conta era `INTERNO` deixou de dar acesso assim que o papel virou `COMUM` no banco, sem esperar a expiração. Provado nas duas frentes: no teste, trocando o papel entre duas requisições com o mesmo token; e na stack, com `UPDATE users SET role='COMUM'` entre duas chamadas — 204, depois 403.

- **C7** — atendido. O caminho inverso também vale: token emitido enquanto a conta era `COMUM` passa a dar acesso quando o papel vira `INTERNO` no banco. Os dois juntos provam que a fonte é o banco, não a claim — um só deles seria compatível com o comportamento antigo.

- **C8** — atendido. A autorização por posse de anúncio não dependia de authorities e continua 403 para conta alheia.

## Observações que não são critério

- A suíte inteira passou sem nenhuma outra alteração. Nenhum teste das nove specs anteriores dependia de as authorities virem da claim, o que indica que a decisão original nunca foi exercitada por nada além dela mesma.
- A consulta por requisição não é custo novo: o filtro já a fazia antes desta spec. O que mudou foi passar a usar o resultado dela.
- Conta apagada do banco com token ainda válido continua respondendo 500: `loadUserByUsername` lança `UsernameNotFoundException` e não há handler. Está declarado no fora de escopo desta spec e segue aberto.
- A claim `role` continua no token e o frontend a usa para desenhar o menu. Uma conta rebaixada continua vendo o item "Painel" até o próximo login — mas ao clicar recebe 403 da API e a tela mostra recusa.
