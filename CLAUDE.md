# Attus Imóveis — Backend

API REST Spring Boot **3.3.5** / Java **21** (virtual threads ligadas), PostgreSQL 16, JWT stateless. Este repo é **metade** do projeto: o frontend vive em `C:\marcos-code\attus-imoveis-front` (Next.js 16 App Router) e os dois são **o mesmo domínio**. Nunca altere um contrato aqui sem atualizar o outro lado.

Atenção: `artifactId` ainda é `demo` e o pacote base é `com.example.demo` — herança do scaffold, não é um repo de exemplo.

## Comandos

### Stack completa em Docker (caminho padrão)

O `docker-compose.yml` **deste repo** orquestra o projeto inteiro — banco, API e o frontend (build context `../attus-imoveis-front`). Exige os dois repositórios lado a lado.

```bash
docker compose up -d --build    # web :3000 · api :8080 · postgres :5433
docker compose logs -f api
docker compose down             # -v também apaga o volume do banco
docker compose build api        # rebuild de um serviço só
```

A API lê `SPRING_DATASOURCE_*` e `JWT_SECRET` do ambiente (relaxed binding sobrescreve o `application.properties`), então dentro da rede do compose ela aponta para `postgres:5432` sem editar arquivo nenhum.

### Local, sem container

```bash
docker compose up -d postgres   # só o banco, em localhost:5433
./mvnw spring-boot:run          # http://localhost:8080
./mvnw test
./mvnw clean package
```

Swagger UI: `http://localhost:8080/swagger-ui.html` (springdoc 2.5.0).
`spring.jpa.hibernate.ddl-auto=update` — **não há migrations**; o schema sai das entidades JPA.

## Arquitetura

Camadas clássicas sob `src/main/java/com/example/demo/`:

- `controllers/` — `@RestController`, prefixo `/api/v1/...`, `@RequiredArgsConstructor`, anotados com `@Tag`/`@Operation` para o Swagger. Controller não tem regra de negócio.
- `services/` — regra de negócio; lança `BusinessException` / `ResourceNotFoundException`.
- `repositories/` — Spring Data JPA.
- `model/` — entidades (`User`, `Announcement`). Getters/setters escritos à mão nas entidades; Lombok fica nos DTOs.
- `dtos/` — mistura deliberada: `record` para entrada/saída de usuário (`UserDTO`, `UserUpdateDTO`, `LoginRequestDTO`), classes `@Getter @Builder` para anúncios (`AnnouncementListDTO` com `fromEntity` estático). Siga o padrão já usado no agregado que estiver mexendo.
- `mappers/` — MapStruct 1.6.3 (`AnnouncementMapper`). Lombok + MapStruct convivem via `lombok-mapstruct-binding`; mexeu na ordem dos `annotationProcessorPaths` do `pom.xml`, rode `./mvnw clean compile`.
- `security/JwtAuthenticationFilter` + `config/SecurityConfig` + `services/JwtService`.
- `exceptions/` — `handler/GlobalExceptionHandler` devolve `models/ApiError`. Erro novo → passa por aqui, nunca `try/catch` solto no controller.
- `enums/` — `PropertyType` (`HOUSE`, `APARTMENT`, `CONDOMINIUM`, `LAND`), `ReasonType` (`SALE`, `RENT`). Persistidos como `EnumType.STRING`; **os mesmos literais estão no Zod do frontend** (`app/schemas/announcement.ts`). Adicionar valor é mudança de contrato nos dois repos.

Entidade nunca vaza para o controller: sempre DTO.

## Segurança

`SecurityConfig`: CSRF off, sessão `STATELESS`, `JwtAuthenticationFilter` antes do `UsernamePasswordAuthenticationFilter`, senha em BCrypt. Rotas públicas:

- `/api/v1/auth/**`
- `POST /api/v1/users`
- `GET /api/v1/announcements` e `GET /api/v1/announcements/*`

Todo o resto exige JWT. Rota nova protegida → nada a fazer (`anyRequest().authenticated()`); rota nova pública → tem que entrar explicitamente nessa lista.

Dono do recurso vem de `@AuthenticationPrincipal UserDetails userDetails` e o service resolve pelo `userDetails.getUsername()` (que é o **e-mail**, o `subject` do token).

## Contrato com o frontend

Base `/api/v1`, consumida por `app/services/api.ts` no front.

| Método | Rota | Auth |
|---|---|---|
| POST | `/auth/authenticate` | pública → `{ token }` |
| POST | `/users` | pública |
| GET | `/users`, `/users/{id}` | JWT |
| PUT/DELETE | `/users/{id}` | JWT |
| GET | `/announcements`, `/announcements/{id}` | pública |
| GET | `/announcements/me` | JWT |
| POST | `/announcements` · PUT `/announcements/{id}` | JWT |

Mudou DTO? Atualize o schema Zod correspondente em `../attus-imoveis-front/app/schemas/` na mesma tarefa.

## Armadilhas conhecidas

- **Duas configurações de CORS** coexistem: `SecurityConfig.corsConfigurationSource()` e `config/CorsConfig` (`WebMvcConfigurer`). Ambas liberam só `http://localhost:3000`. Ao mexer em CORS, trate as duas ou consolide numa.
- **Claim `fullName` vs `fullname`**: `JwtService.generateToken` emite `fullName`, mas o front decodifica esperando `fullname` (`app/contexts/auth-context.tsx`). Alinhe os dois lados em vez de remendar só aqui.
- `jwt.secret` está em claro no `application.properties` (marcado como exemplo) — não adicione segredo real ao repo.
- Token expira em 1h, sem refresh token.
- `GET /announcements/me` é declarado **depois** de `GET /{id}` no controller; ao adicionar rotas literais, cuidado com colisão com o path variable.
- `DELETE /announcements/{id}` existe comentado no controller — está fora do contrato hoje.
- `src/test` tem só o smoke test do contexto; qualquer service novo deve vir com teste unitário.
