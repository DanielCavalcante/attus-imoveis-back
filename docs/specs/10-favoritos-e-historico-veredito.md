# Veredito — 10-favoritos-e-historico

Base: estado da árvore após a spec 09, mais os arquivos novos de `.` (nada commitado desde `1e03b12`).

> **Limitação de independência.** Produzido pela mesma sessão que escreveu o código, a pedido do humano. O passo 2 do `verificar` pede contexto limpo porque quem escreve o código escreve os testes. Leia sabendo que essa garantia não existe aqui.

- **C1** — atendido. `UserLibraryService.favorite` grava o favorito e o teste conta a linha no banco.

- **C2** — atendido. O serviço consulta o par conta/anúncio antes de gravar e retorna sem fazer nada se já existir. O par também é único no banco, então uma corrida não produz duplicata — ela falharia na constraint em vez de gravar duas linhas.

- **C3** — atendido. `unfavorite` remove quando existe e não faz nada quando não existe, sem erro: desfavoritar o que já não é favorito é o mesmo resultado desejado.

- **C4** — atendido. A consulta seleciona os anúncios pelo favorito da conta e o teste casa o id do anúncio.

- **C5** — atendido. A consulta filtra por `f.user.email`, então o favorito do vizinho não aparece; a lista da segunda conta volta vazia.

- **C6** — atendido. `POST /{id}/favorito` não está entre as rotas liberadas (o `permitAll` de announcements cobre só `GET`), então cai em `anyRequest().authenticated()`.

- **C7** — atendido. `requireAnnouncement` lança `ResourceNotFoundException` → 404, que passou a ser 404 de verdade desde a spec 02.

- **C8** — atendido. A listagem reaproveita `AnnouncementListDTO`, a mesma forma do card; o teste confere título, cidade, preço e imagem, não apenas o id.

- **C9** — atendido. `GET /api/v1/announcements/{id}` chama `registerView` com o e-mail do principal e a linha aparece no banco.

- **C10** — atendido. `registerView` procura o par existente antes de criar; a segunda visita reaproveita a linha.

- **C11** — atendido. `viewedAt` é reescrito em toda visita, e o teste compara o instante antes e depois.

- **C12** — atendido. A rota é pública: sem token o principal chega nulo, `registerView` retorna na primeira linha e nada é gravado. A leitura do anúncio continua respondendo 200 — o histórico não pode quebrar a página pública.

- **C13** — atendido. A consulta ordena por `viewedAt desc`, e o teste visita dois anúncios em ordem e confirma a lista invertida.

- **C14** — atendido. `/me/historico` tem dois segmentos após o prefixo, então não casa com o `permitAll` de um segmento e exige sessão.

- **C15** — atendido. `Announcement` ganhou as duas coleções com cascade e `orphanRemoval`; remover o anúncio leva favoritos e histórico junto.

## Observações que não são critério

- O registro de visita é síncrono dentro da leitura do anúncio. Toda abertura de detalhe por conta autenticada custa uma escrita no banco, no caminho da requisição. Enquanto o volume for pequeno não incomoda; com tráfego, isso vira fila.
- `registerView` está anotado `@Transactional` e é chamado depois de a leitura já ter acontecido. Se a escrita falhar, a exceção sobe e a página de detalhe responde erro mesmo tendo os dados — o isolamento pretendido no contrato técnico vale para a ausência de sessão, não para falha de banco.
- O histórico não tem limite de tamanho nem expurgo. Uma conta antiga acumula uma linha por anúncio visitado, para sempre.
- Não existe rota que diga se um anúncio específico está favoritado. A tela de detalhe precisa carregar a lista inteira de favoritos para desenhar o estado do botão.
