# Encontrei — API

Backend do **Encontrei**, portal de imóveis de João Pessoa e da Paraíba.

Spring Boot **3.3.5** · Java **21** (virtual threads) · PostgreSQL **16** · JWT stateless.

> Este repositório é **metade** do projeto. O frontend vive em
> [`attus-imoveis-front`](https://github.com/DanielCavalcante/attus-imoveis-front)
> e os dois são **o mesmo domínio**: enum, DTO e rota mudam nos dois lados na
> mesma tarefa, nunca num só.

Herança do scaffold que não vale a pena desfazer: o `artifactId` ainda é `demo`
e o pacote base é `com.example.demo`. Não é um repositório de exemplo.

---

## Subir o projeto

O `docker-compose.yml` **deste repositório** orquestra tudo — banco, API e o
site. Ele usa `../attus-imoveis-front` como contexto de build, então os dois
repositórios precisam estar lado a lado.

```bash
git clone git@github.com:DanielCavalcante/attus-imoveis-back.git
git clone git@github.com:DanielCavalcante/attus-imoveis-front.git
cd attus-imoveis-back
docker compose up -d --build
```

| | |
|---|---|
| site | http://localhost:3000 |
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Postgres | `localhost:5433` |

```bash
docker compose logs -f api
docker compose build api      # reconstrói um serviço só
docker compose down           # `-v` também apaga o volume do banco
```

### Sem container

```bash
docker compose up -d postgres   # só o banco
./mvnw spring-boot:run
./mvnw test
```

## Configuração

**Não há valor de produção embutido no código.** `application.properties` só
declara as variáveis, sem padrão: a aplicação recusa subir sem elas, que é o
comportamento desejado — um padrão silencioso faria produção apontar para
desenvolvimento sem ninguém notar.

| variável | |
|---|---|
| `SPRING_DATASOURCE_URL` · `_USERNAME` · `_PASSWORD` | banco |
| `JWT_SECRET` | chave de assinatura do token |
| `ATTUS_IMAGENS_BUCKET` | bucket GCS das fotos |
| `ATTUS_CORS_ORIGENS` | origens liberadas; **vazio recusa todas** |
| `PORT` | porta HTTP (padrão 8080) |

O perfil `dev` (`SPRING_PROFILES_ACTIVE=dev`, que o compose já define) traz os
valores de desenvolvimento. É o único lugar onde eles existem.

## Arquitetura

Camadas clássicas sob `src/main/java/com/example/demo/`:

| pasta | responsabilidade |
|---|---|
| `controllers/` | `@RestController` em `/api/v1/…`. Sem regra de negócio. |
| `services/` | a regra. Lança `BusinessException` / `ResourceNotFoundException`. |
| `repositories/` | Spring Data JPA. |
| `model/` | entidades. |
| `dtos/` | `record` para usuário, `@Builder` para anúncio — siga o agregado que estiver mexendo. |
| `mappers/` | MapStruct 1.6.3. |
| `security/`, `config/` | filtro JWT, `SecurityConfig`, CORS. |
| `exceptions/` | `GlobalExceptionHandler` → `ApiError`. Erro novo passa por aqui. |

Entidade nunca vaza para o controller: sempre DTO.

### Banco

**Flyway**, `V1`…`V11` em `src/main/resources/db/migration/`, com
`ddl-auto=validate`. O schema não sai das entidades: mudança de modelo exige
migração nova, e o `validate` reprova a subida se as duas divergirem.

### Segurança

Sessão `STATELESS`, CSRF desligado, senha em BCrypt, `JwtAuthenticationFilter`
antes do filtro de usuário e senha. Público, e só:

- `/api/v1/auth/**`
- `POST /api/v1/users`
- `GET /api/v1/announcements`, `GET /api/v1/announcements/{id}`
- `GET /api/v1/plans`
- `GET /api/v1/imagens/{nome}`
- `/actuator/health/**`

Todo o resto exige JWT — `anyRequest().authenticated()`. **Rota nova protegida
não precisa de nada; rota nova pública precisa entrar nessa lista.**

Dono do recurso vem do token, nunca do corpo: o service resolve pelo
`userDetails.getUsername()`, que é o e-mail no `subject`. Em `/users/{id}` o
`id` do caminho é conferido contra o da sessão — uma pessoa mexe apenas no
próprio cadastro.

## Rotas

| | rota | auth |
|---|---|---|
| POST | `/api/v1/auth/authenticate` | pública → `{ token }` |
| POST | `/api/v1/users` | pública |
| GET PUT DELETE | `/api/v1/users/{id}` | JWT — **só o próprio** |
| GET | `/api/v1/users` | papel `INTERNO` |
| GET | `/api/v1/announcements` · `/{id}` · `/cidades` · `/bairros` | pública |
| GET | `/api/v1/announcements/me` · `/me/quota` · `/me/favoritos` · `/me/historico` | JWT |
| POST PUT | `/api/v1/announcements` · `/{id}` | JWT |
| PATCH | `/api/v1/announcements/{id}/inativar` · `/reativar` | JWT |
| POST DELETE | `/api/v1/announcements/{id}/favorito` | JWT |
| GET PUT | `/api/v1/companies/me` | JWT |
| POST | `/api/v1/companies/me/analise` | JWT |
| GET | `/api/v1/plans` | pública |
| GET | `/api/v1/enderecos/{cep}` | JWT |
| POST GET | `/api/v1/imagens` · `/{nome}` | envio com JWT, leitura pública |
| — | `/api/v1/backoffice/**` | papel `INTERNO` |
| GET | `/actuator/health/liveness` · `/readiness` | sondas |

Enums compartilhados com o front, literal por literal:
`PropertyType` = `HOUSE · APARTMENT · CONDOMINIUM · LAND`;
`ReasonType` = `SALE · RENT`.

## Regras que não são óbvias no código

- **Empresa PJ só anuncia depois de aprovada _e_ com plano vigente.** Aprovação
  sem plano não libera.
- **Rebaixar plano não mexe em anúncio existente.** O administrador é avisado de
  que a empresa ficará acima da cota, e nada é bloqueado nem apagado.
- **Imagem não vem por URL.** Sobe por `multipart` e vai para o bucket; o
  anúncio guarda o caminho. Anúncio anterior a essa regra mantém a URL antiga e
  continua editável.
- **CEP consulta o cache local antes do provedor externo**, e a falha do
  provedor nunca derruba o cadastro.

## Testes

```bash
./mvnw test
```

Testcontainers sobe Postgres e um emulador de GCS de verdade — não há mock de
banco. Precisa de Docker no ar.

`src/test/resources/application-test.properties` (e **não** `application.properties`:
esse nome sombreia o arquivo principal e a suíte passaria a medir a própria
configuração de teste em vez da de produção).

## Produção

Google Cloud, `terraform/`. A forma é uma só e é deliberada:

```
internet ──▶ site (Cloud Run, público)
                │  VPC
                ▼
             API (Cloud Run, entrada interna)
                │  IP privado
                ▼
             Cloud SQL (sem IP público)
```

**A API não tem entrada da internet e o banco não tem IP público.** O único
ponto de entrada é o site. Duas trancas na API: entrada interna *e* invoker
nominal — só a conta de serviço do site.

Segredo nenhum mora no estado do Terraform: a senha do banco é gerada no apply
e a chave JWT é gravada fora dele, direto no Secret Manager.

```bash
bash terraform/subir.sh
```

Seis passos, idempotente, retoma de onde parou. Detalhes em
[`terraform/README.md`](terraform/README.md).

## Armadilhas conhecidas

- **Duas configurações de CORS** coexistem — `SecurityConfig` e `config/CorsConfig`.
  Mexeu em uma, trate a outra.
- **`fullName` no token, `fullname` no front.** O `JwtService` emite a claim
  como `fullName` e o contexto de autenticação do site lê `fullname`. Alinhe os
  dois lados em vez de remendar um.
- **`GET /announcements/me` é declarado depois de `GET /{id}`.** Rota literal
  nova corre risco de colidir com o path variable.
- **O token expira em 1 h e não há refresh.** A sessão simplesmente acaba.
