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
- [ ] **C7** `dominio` — recusa não deixa objeto gravado no bucket.
- [ ] **C8** `seguranca` — o nome do objeto no bucket é gerado pelo servidor; o nome enviado por quem sobe não decide caminho nem extensão.
- [ ] **C9** `contrato-api` — a rota que serve a imagem devolve o `Content-Type` do que foi guardado.
- [ ] **C10** `seguranca` — a resposta que serve a imagem traz `X-Content-Type-Options: nosniff`.
- [ ] **C11** `seguranca` — criar anúncio com `image` começando em `http://` ou `https://` é recusado.
- [ ] **C12** `seguranca` — editar anúncio trocando `image` por endereço externo é recusado.
- [ ] **C13** `dominio` — anúncio gravado antes desta spec, com endereço externo em `image`, continua sendo devolvido pela API como está.
- [ ] **C14** `persistencia` — imagem enviada continua acessível depois de o contêiner da API ser recriado.
- [ ] **C15** `integracao` — a aplicação fala com o emulador local em desenvolvimento e com o Cloud Storage em produção sem troca de código, só de configuração.

## Contrato técnico

Os bytes vão para um **bucket do Google Cloud Storage**, nunca para o disco do
contêiner nem para o banco. Disco de contêiner morre com ele e não sobrevive a
mais de uma instância da API; `bytea` inflaria dump e backup sem dar nada em
troca.

Em desenvolvimento o bucket é o **fake-gcs-server** (`fsouza/fake-gcs-server`),
que fala o protocolo do GCS: o mesmo código, o mesmo cliente, só o endereço
muda. É isso que a C15 mede -- emulador que exigisse um caminho de código
próprio deixaria o caminho de produção sem nenhum teste.

- cliente: `com.google.cloud:google-cloud-storage`;
- em dev, o cliente aponta para o emulador e usa credencial vazia
  (`NoCredentials`), porque o emulador não autentica;
- em produção vale a credencial padrão do ambiente (ADC). **Nenhuma chave de
  serviço entra no repositório**, e nem precisa: no GCP a identidade vem da
  conta de serviço da carga.
- nos testes, o emulador sobe por Testcontainers
  (`io.aiven:testcontainers-fake-gcs-server`), como o Postgres já faz.

**A imagem é servida pela API, não pelo bucket.** O bucket fica privado, e é o
que mantém C9 e C10 verdadeiros -- `Content-Type` e `nosniff` são nossos. O
custo é banda passando pela API, e a saída, se isso apertar, é URL assinada;
não é abrir o bucket.

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
- **URL assinada e bucket servido direto ao navegador.** Enquanto a API serve,
  o `Content-Type` e o `nosniff` são nossos; URL assinada é a saída para
  banda, e vira decisão quando houver o problema.
- **Provisionar o bucket no GCP** (nome, região, ciclo de vida, IAM). É
  infraestrutura, não código, e a spec só exige que o nome venha de
  configuração.
- **Várias imagens por anúncio** — é a spec 34. Esta fecha o envio de uma.
- **A tela de envio** — é a spec 33, no front.

## Plano

1. Dependência do cliente, propriedades de bucket e limite, e o emulador no
   compose — `pom.xml`, `application.properties`, `docker-compose.yml`.
2. Configuração do cliente, com emulador em dev e ADC em produção —
   `config/StorageConfig.java`.
3. Guarda e validação do arquivo — `services/ImagemService.java`.
4. Rotas de envio e de entrega — `controllers/ImagemController.java`.
5. Recusa de endereço externo na entrada do anúncio —
   `dtos/AnnouncementCreateDTO.java`, `dtos/AnnouncementUpdateDTO.java`.
6. Testes por critério, com o emulador por Testcontainers —
   `src/test/java/com/example/demo/ImagemDoAnuncioSpecTest.java`.

## Perguntas em aberto

Nenhuma.
