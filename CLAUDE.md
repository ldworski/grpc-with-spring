# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Early-stage Spring Boot app: an order system (users order products) exposed via gRPC and backed by MongoDB. So far it
has the domain model, the persistence layer (ports + Mongo adapter) and the create-user and create-product use cases
with their gRPC endpoints (`UserService` in `src/main/proto/user.proto`, `ProductService` in
`src/main/proto/product.proto`). `HelloGrpcService` (`HelloService` in `hello.proto`, in `adapter.in.grpc`) is a
leftover hello-world endpoint.

## Architecture (hexagonal / ports and adapters)

- `domain.model`: plain Java records `Product`, `User`, `Order`, `OrderItem` with invariants in compact constructors. No
  Spring or Mongo annotations. `id` is `null` until persisted.
- `domain.port.in`: inbound ports (use cases, e.g. `CreateUserUseCase` with a nested `CreateUserCommand`), to be called
  by inbound adapters (gRPC endpoints).
- `domain.service`: use case implementations (e.g. `CreateUserService`). They depend only on ports and have no Spring
  annotations. They are registered as beans in `config.DomainConfiguration`.
- `domain.exception`: domain exceptions (e.g. `UserAlreadyExistsException`, thrown when the email is already taken).
- `domain.port.out`: outbound ports (`ProductRepository`, `UserRepository`, `OrderRepository`) that use only domain
  types.
- `adapter.out.persistence.mongo`: implements the ports with blocking Spring Data `MongoRepository`. All `@Document`
  classes (records with `fromDomain`/`toDomain`) live in the `document` subpackage and are public because they are used
  across packages. Spring Data repositories (`*MongoRepository`) and `*PersistenceAdapter`s are package-private in
  `mongo`, so the rest of the app sees only the ports. `OrderItem`s are embedded in the order document, and users and
  products are referenced by id (no `@DBRef`). `User.balance` is stored as `Decimal128`, and `email` has a unique index.
  Indexes declared with `@Indexed` are created on startup (`spring.data.mongodb.auto-index-creation: true`).
  `MongoConfiguration` disables the Spring Data `_class` type key: a `BeanPostProcessor` sets
  `DefaultMongoTypeMapper(null)` on every `MappingMongoConverter`. Documents are not polymorphic, so reads use the
  declared type.
- `adapter.in.grpc`: gRPC endpoints (e.g. `UserGrpcService` for `user.proto`). They are package-private `@Service`
  classes that depend only on inbound ports and domain types, never on domain services or outbound ports. Proto ↔ domain
  mapping lives in public `*GrpcMapper` utility classes in the `adapter.in.grpc.mapper` subpackage. Domain exceptions
  map to gRPC statuses in the endpoint: `UserAlreadyExistsException` → `ALREADY_EXISTS`, `IllegalArgumentException`
  (domain validation, unparsable numbers) → `INVALID_ARGUMENT`. Money goes over the wire as a decimal `string`, never as
  `double`. Required numeric request fields are declared `optional` in proto (e.g. `initial_stock_quantity`), so the
  mapper can tell a missing value from `0` via `has...()` and reject it with `IllegalArgumentException` (→
  `INVALID_ARGUMENT`).

## Stack

- Java 25 (Gradle toolchain), Gradle 9.8 wrapper, Kotlin DSL build (`build.gradle.kts`)
- Spring Boot 4.1.2 with `spring-boot-starter-grpc-server` (Spring gRPC)
- MongoDB via `spring-boot-starter-mongodb` plus both blocking (`spring-boot-starter-data-mongodb`) and reactive
  (`spring-boot-starter-data-mongodb-reactive`) Spring Data starters. Only the blocking API is used so far (persistence
  adapters use `MongoRepository`). In Boot 4 connection settings use the `spring.mongodb.*` prefix (not
  `spring.data.mongodb.*`, which only holds Spring Data options like `auto-index-creation`).
- `com.google.protobuf` Gradle plugin: `.proto` files go in `src/main/proto`, Java classes are generated into package
  `pl.dworski.grpcwithspring.proto`. The Spring Boot plugin sets the `protoc` / `protoc-gen-grpc-java` versions itself;
  the build only declares `protobuf { plugins { id("grpc") } }` (without it no `*Grpc` stubs are generated). Don't add
  artifact versions or `generateProtoTasks` for the `grpc` plugin, as that conflicts with Boot's configuration.
- Base package: `pl.dworski.grpcwithspring`

## Profiles and MongoDB

- `application.yaml` sets the `spring.mongodb.*` connection settings to env-var placeholders (`MONGODB_HOST`,
  `MONGODB_PORT`, `MONGODB_DATABASE`, `MONGODB_USERNAME`, `MONGODB_PASSWORD`, `MONGODB_AUTH_DATABASE`), so starting
  without these variables and without a profile that overrides them fails by design. Never put real credentials there.
- `prod` is the default profile (`spring.profiles.default` in `application.yaml`). There is no `application-prod.yaml`:
  in prod the connection comes from the env-var placeholders in `application.yaml`.
- `local` (`application-local.yaml`) overrides the placeholders and connects to the MongoDB from `compose.yaml`
  (`docker compose up -d`); credentials there must match the compose root user.
- `test` (`src/test/resources/application-test.yaml`) is only for Spring Boot integration tests. It overrides database
  and credentials; host and port come from the container (see Testing).
- MongoDB images: use `mongo:9.0` (compose and tests). `mongo:8.0` refuses to start on the dev machine's Linux kernel
  7.x (SERVER-121912).

## Testing

- Tests are written in Groovy with Spock (`src/test/groovy`). Spring Boot integration specs require a running Docker
  daemon (Testcontainers).
- `testLogging` in `build.gradle.kts` prints test stdout/stderr (Spring, Testcontainers logs), per-test results and full
  stack traces to the console. When nothing changed, Gradle marks `test` up-to-date and shows nothing. Use
  `./gradlew test --rerun` to run the tests again anyway.
- Domain services get plain unit specs without Spring or Docker (e.g. `CreateUserServiceSpec`): construct the service
  directly and mock its outbound ports with Spock `Mock()`.
- Spring Boot integration specs use `@SpringBootTest`, `@ActiveProfiles('test')` and
  `@Import(MongoTestContainerConfiguration)`. That configuration starts a `GenericContainer` with `mongo:9.0` whose root
  user is created from the `spring.mongodb.username/password` of the `test` profile (single source of credentials), and
  registers the container's host/mapped port via a `DynamicPropertyRegistrar`. `MongoDBContainer` from
  `testcontainers-mongodb` is deliberately not used: it runs without authentication.
- Every `@SpringBootTest` spec also needs `@AutoConfigureTestGrpcTransport` (in-process transport, no port). Without it
  the context starts a Netty gRPC server on 9090 and fails with `BindException` whenever the app is already running
  locally. Specs with an identical annotation set share one cached context and one Mongo container. Each different
  `@ImportGrpcClients` set (`HelloGrpcServiceSpec`, `UserGrpcServiceSpec`, `ProductGrpcServiceSpec`) starts its own
  context and container.
- Clean up entity collections with `mongoTemplate.remove(new Query(), ...)`, never `dropCollection`. Indexes (e.g.
  unique `users.email`) are created only once, at context startup, and the context is shared, so dropping a collection
  silently removes its indexes for every spec that runs later.
- gRPC tests additionally use `@ImportGrpcClients(types = ...BlockingStub)` to inject client stubs.

## Commands

```bash
./gradlew build                     # compile (incl. protobuf codegen) + tests
docker compose up -d                # start local MongoDB
./gradlew bootRun --args='--spring.profiles.active=local'   # run against local MongoDB
./gradlew test                      # all tests (Spock on JUnit Platform)
./gradlew generateProto             # regenerate Java sources from .proto files
```
