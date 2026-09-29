# 32-imagem-enviada-e-nao-linkada

## Intenção
A imagem do anúncio passa a ser um arquivo que o anunciante envia, e o servidor
para de aceitar endereço apontando para fora.

## Depende de
28-aprovacao-manda-no-anuncio

## Critérios

- [ ] **C1** `contrato-api` — conta autenticada envia um arquivo de imagem e recebe de volta o caminho por onde ele será servido.
- [ ] **C2** `seguranca` — a mesma requisição sem token é recusada.
- [ ] **C3** `seguranca` — arquivo cujo tipo declarado não é imagem permitida é recusado.
- [ ] **C4** `seguranca` — arquivo SVG é recusado mesmo declarando-se imagem.
- [ ] **C5** `seguranca` — arquivo que se diz PNG mas cuja assinatura de bytes não é de PNG é recusado.
- [ ] **C6** `seguranca` — arquivo acima do limite de tamanho é recusado.
- [ ] **C7** `dominio` — recusa não deixa arquivo gravado no disco.
- [ ] **C8** `seguranca` — o nome do arquivo em disco é gerado pelo servidor; nome enviado por quem sobe não decide caminho nem extensão.
- [ ] **C9** `contrato-api` — a rota que serve a imagem devolve o `Content-Type` do que foi guardado.
- [ ] **C10** `seguranca` — a resposta que serve a imagem traz `X-Content-Type-Options: nosniff`.
- [ ] **C11** `seguranca` — criar anúncio com `image` começando em `http://` ou `https://` é recusado.
- [ ] **C12** `seguranca` — editar anúncio trocando `image` por endereço externo é recusado.
- [ ] **C13** `dominio` — anúncio gravado antes desta spec, com endereço externo em `image`, continua sendo devolvido pela API como está.
- [ ] **C14** `persistencia` — imagem enviada continua acessível depois de o contêiner da API ser recriado.

## Contrato técnico

Os bytes vão para o disco, num volume nomeado do compose, e não para o banco:
imagem em `bytea` infla dump e backup sem dar nada em troca aqui, e não há
serviço de objeto no projeto para introduzir junto.

Três checagens, e as três são necessárias porque cada uma cobre o que a outra
deixa passar: o `Content-Type` declarado (mente), a extensão (mente) e a
assinatura de bytes do arquivo (é a que decide). SVG fica fora da lista mesmo
sendo imagem — SVG é documento com script dentro, e servi-lo do próprio
domínio seria XSS hospedado por nós.

Tipos aceitos: JPEG, PNG e WebP. Teto de 5 MB, em propriedade, como o da quota.

`image` continua sendo texto e continua com o mesmo tamanho: o que muda é o que
pode entrar nele. Recusar `http://` e `https://` na entrada é o que torna a
mudança segurança, e não só interface -- sem isso, bastaria não usar a tela.

## Fora de escopo

- **Migrar os anúncios que já têm endereço externo.** Eles continuam como
  estão, por C13. Derrubar imagem de quem já anuncia é efeito grande, e o que
  o pedido fecha é a porta de entrada.
- **A foto de perfil da conta**, que também é texto livre hoje. Mesmo problema,
  outra tela e outro fluxo; vira spec própria se você quiser.
- **Redimensionar, comprimir ou gerar miniatura.** O teto de tamanho resolve o
  problema de hoje; processar imagem traz dependência nova.
- **A tela de envio** — é a spec 33, no front.

## Plano

1. Propriedades de limite e diretório — `application.properties`, `docker-compose.yml`.
2. Guarda e validação do arquivo — `services/ImagemService.java`.
3. Rotas de envio e de entrega — `controllers/ImagemController.java`.
4. Recusa de endereço externo na entrada do anúncio —
   `dtos/AnnouncementCreateDTO.java`, `dtos/AnnouncementUpdateDTO.java`.
5. Testes por critério — `src/test/java/com/example/demo/ImagemDoAnuncioSpecTest.java`.

## Perguntas em aberto

Nenhuma.
