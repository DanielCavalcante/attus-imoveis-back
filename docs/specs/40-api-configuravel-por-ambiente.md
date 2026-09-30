# 40-api-configuravel-por-ambiente

## Intenção
A API deixa de carregar desenvolvimento embutido e passa a ser configurada
inteiramente pelo ambiente — condição para existir um ambiente de produção.

## Depende de
nenhuma

## Critérios

- [ ] **C1** `seguranca` — sem `JWT_SECRET` no ambiente, a aplicação não sobe.
- [ ] **C2** `seguranca` — `JWT_SECRET` com menos de 32 bytes é recusado na subida, com mensagem que diz o mínimo.
- [ ] **C3** `seguranca` — as credenciais do banco vêm do ambiente, e a ausência delas derruba a subida em vez de tentar `localhost`.
- [ ] **C4** `seguranca` — o nome do bucket vem do ambiente, e a ausência dele derruba a subida.
- [ ] **C5** `seguranca` — as origens aceitas pelo CORS vêm do ambiente.
- [ ] **C6** `seguranca` — sem origem configurada, nenhuma origem é liberada — e em nenhum caso o valor vira `*`.
- [ ] **C7** `seguranca` — sem as variáveis de bootstrap, a aplicação sobe e não cria conta interna nenhuma.
- [ ] **C8** `observabilidade` — existe rota de saúde que responde 200 sem autenticação e sem tocar o banco.
- [ ] **C9** `observabilidade` — existe rota de prontidão que responde erro enquanto o banco não responde.
- [ ] **C10** `seguranca` — nenhuma das duas rotas expõe versão, dependências ou configuração.
- [ ] **C11** `observabilidade` — fora de desenvolvimento, o log sai em JSON, um evento por linha.
- [ ] **C12** `observabilidade` — fora de desenvolvimento, o SQL não é registrado no log.
- [ ] **C13** `dominio` — a porta do servidor vem de `PORT`, com 8080 como padrão.
- [ ] **C14** `persistencia` — contra uma base vazia, as migrações aplicam da V1 à última sem baseline.

## Contrato técnico

**O que o repositório tem hoje, e que cada critério desfaz:**
`jwt.secret=33YAIuZ5P9QDy3rysBBLAEFglXNLsAHQ` está commitado como padrão --
subir em produção sem a variável assina token com uma chave que qualquer um
lê no Git, e quem a tem forja qualquer sessão. O CORS tem
`allowedOrigins("http://localhost:3000")` em código. O bucket tem padrão
`attus-imoveis-dev`. O datasource aponta para `localhost:5433`.

**Falhar na subida é o ponto, não um efeito colateral.** Um padrão de
desenvolvimento que sobrevive ao deploy não avisa: ele funciona, e o defeito
só aparece quando alguém percebe que a chave é pública. Sobe quebrado e
barulhento ou não sobe.

C6 separa duas coisas que se confundem: lista vazia recusa todas as origens;
`*` aceita todas. Num serviço com token no navegador, `*` é o contrário do que
"sem configuração" deveria significar.

C8 e C9 são rotas distintas de propósito. Saúde responde "o processo está de
pé" e não pode depender do banco -- se dependesse, uma indisponibilidade do
banco derrubaria e recriaria o contêiner em ciclo. Prontidão responde "posso
atender" e depende. Quem orquestra usa uma para reiniciar e a outra para
rotear.

Actuator é o caminho natural para C8 e C9, e então C10 deixa de ser detalhe:
os endpoints além de saúde ficam desligados, e `health` não mostra detalhe
para quem não está autenticado.

## Fora de escopo

- **O Terraform.** Esta spec prepara a aplicação para ser configurada; escrever
  a infraestrutura é a spec 42, e ela depende desta — não dá para declarar
  variável de ambiente para um serviço que ainda não as aceita.
- **Provisionar bucket, banco e registro de imagens.** Infraestrutura, mesma
  spec 42.
- **Trocar a chave de assinatura por rotação automática.**
- **Tirar o `InternoDev123` do `docker-compose.yml`.** Aquele arquivo é o
  ambiente de desenvolvimento e você decidiu mantê-lo; o que C7 garante é que
  a ausência das variáveis não cria conta nenhuma, que é o que vale em
  produção.
- **Mudar o perfil de desenvolvimento.** `docker compose up` continua
  funcionando sem ninguém exportar variável.

## Plano

1. Perfis: o que hoje é padrão vira explícito por ambiente —
   `application.properties`, `application-dev.properties`.
2. Validação na subida, com mensagem que diz o que falta —
   `config/ConfiguracaoObrigatoria.java`.
3. CORS por ambiente — `config/CorsConfig.java`.
4. Saúde e prontidão — `pom.xml` (actuator), `config/SecurityConfig.java`.
5. Log em JSON fora de desenvolvimento — `src/main/resources/logback-spring.xml`.
6. Testes por critério —
   `src/test/java/com/example/demo/ConfiguracaoDeAmbienteSpecTest.java`.

## Perguntas em aberto

Nenhuma.
