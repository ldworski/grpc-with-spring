# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Early-stage Spring Boot app: an order system (users order products) exposed via gRPC and backed by MongoDB. So far it
has the domain model, the persistence layer (ports + Mongo adapter) and use cases with gRPC endpoints: create-user
(`UserService` in `src/main/proto/user.proto`), create-product and paged get-products (`ProductService` in
`src/main/proto/product.proto`), and place-order (`OrderService` in `src/main/proto/order.proto`).
`HelloGrpcService` (`HelloService` in `hello.proto`, in `adapter.in.grpc`) is a leftover hello-world endpoint.

## Architecture (hexagonal / ports and adapters)

- `domain.model`: plain Java records `Product`, `User`, `Order`, `OrderItem` with invariants in compact constructors. No
  Spring or Mongo annotations. `id` is `null` until persisted.
- `domain.model.paging`: pagination types `PageQuery` (page from 0, default size 20, size above 50 is capped to 50,
  `PageQuery.of(null, null)` gives the defaults) and `Page<T>` (with computed `totalPages()`). Ports use these, never
  Spring Data's `Pageable`/`Page`. Persistence adapters sort paged results by `id`.
- `domain.port.in`: inbound ports (use cases), to be called by inbound adapters (gRPC endpoints). Each use case has its
  own interface with a nested input record: `*Command` for writes (e.g. `CreateUserUseCase.CreateUserCommand`),
  `*Query` for reads (e.g. `GetProductsUseCase.GetProductsQuery`).
- `domain.service`: use case implementations (e.g. `CreateUserService`). They depend only on ports and have no Spring
  annotations. They are registered as beans in `config.DomainConfiguration`.
- `domain.exception`: domain exceptions (e.g. `UserAlreadyExistsException`, thrown when the email is already taken;
  `UserNotFoundException`, `ProductNotFoundException`, `InsufficientStockException` from placing an order;
  `TransactionConflictException` when a transaction keeps conflicting after all retries).
- `domain.port.out`: outbound ports (`ProductRepository`, `UserRepository`, `OrderRepository`) that use only domain
  types, plus `TransactionRunner` (`inTransaction(Supplier)`). Domain services that must be atomic run their whole body
  inside `transactionRunner.inTransaction(...)`, and any exception thrown there rolls everything back. This is a
  deliberate choice over `@Transactional` on domain services: it keeps the domain free of `spring-tx`, works without a
  Spring proxy, and lets unit specs verify that repositories are accessed only inside the transaction.
- Placing an order (`PlaceOrderService`): inside one transaction it checks that the user exists, then for each item
  calls `ProductRepository.decreaseStockIfAvailable`, a single conditional update (`stockQuantity >= quantity` filter
  plus `$inc`) that returns `false` instead of decrementing when stock is short. The service never does read-check-write
  on
  stock. After a `false` it reads the product only to choose between `ProductNotFoundException` and
  `InsufficientStockException`. `PlaceOrderCommand` rejects a blank user id, an empty item list and repeated product
  ids, and `OrderItem` rejects a blank product id. Proto sends a missing string as `""`, not `null`, so blank checks in
  the domain are what turn missing ids into `INVALID_ARGUMENT` instead of `NOT_FOUND`.
- `adapter.out.persistence.mongo`: implements the ports with blocking Spring Data `MongoRepository`. All `@Document`
  classes (records with `fromDomain`/`toDomain`) live in the `document` subpackage and are public because they are used
  across packages. Spring Data repositories (`*MongoRepository`) and `*PersistenceAdapter`s are package-private in
  `mongo`, so the rest of the app sees only the ports. `OrderItem`s are embedded in the order document, and users and
  products are referenced by id (no `@DBRef`). `User.balance` is stored as `Decimal128`, and `email` has a unique index.
  Indexes declared with `@Indexed` are created on startup (`spring.data.mongodb.auto-index-creation: true`).
  `MongoConfiguration` disables the Spring Data `_class` type key: a `BeanPostProcessor` sets
  `DefaultMongoTypeMapper(null)` on every `MappingMongoConverter`. Documents are not polymorphic, so reads use the
  declared type. `MongoConfiguration` also defines the `MongoTransactionManager` (Boot does not auto-configure one), and
  `MongoTransactionRunner` implements `TransactionRunner` with a `TransactionTemplate` over it. Both repositories and
  `MongoTemplate` join that transaction. Concurrent transactions writing the same document abort the later one with a
  `WriteConflict` (label `TransientTransactionError`) instead of waiting. `MongoTransactionRunner` therefore wraps the
  transaction in a Spring Framework `RetryTemplate` (`org.springframework.core.retry`) that re-runs the whole
  transaction only when an exception in the cause chain carries that label (up to 4 retries, exponential backoff with
  jitter). It calls `RetryTemplate.invoke(Supplier)` (since Framework 7.0.3), which rethrows the last original
  exception. Don't switch to `execute(Retryable)`: it wraps every failure, including non-retryable domain exceptions,
  in a checked `RetryException` that would have to be unwrapped. When a transient error is still thrown after the
  retries, the runner converts it into the domain `TransactionConflictException` (part of the `TransactionRunner`
  contract, so callers never see Spring or Mongo exceptions). Other exceptions, including domain ones, propagate
  unchanged and are never retried. `UnknownTransactionCommitResult` is deliberately not retried, because the commit may
  have succeeded and re-running would decrement stock twice.
- `adapter.in.grpc`: gRPC endpoints (e.g. `UserGrpcService` for `user.proto`). They are package-private `@Service`
  classes that depend only on inbound ports and domain types, never on domain services or outbound ports. Proto ↔ domain
  mapping lives in public `*GrpcMapper` utility classes in the `adapter.in.grpc.mapper` subpackage. Domain exceptions
  map to gRPC statuses in the endpoint: `UserAlreadyExistsException` → `ALREADY_EXISTS`,
  `UserNotFoundException`/`ProductNotFoundException` → `NOT_FOUND`, `InsufficientStockException` →
  `FAILED_PRECONDITION`, `TransactionConflictException` → `ABORTED`, `IllegalArgumentException`
  (domain validation, unparsable numbers) → `INVALID_ARGUMENT`. Money goes over the wire as a decimal `string`, never as
  `double`. Numeric request fields where a missing value differs from `0` are declared `optional` in proto, so the
  mapper can check `has...()`: required ones (e.g. `initial_stock_quantity`) are rejected with
  `IllegalArgumentException` (→ `INVALID_ARGUMENT`), and paging fields (`page`, `page_size`) fall back to the
  `PageQuery` defaults. Paged responses return the size actually used (after capping) and
  `total_elements`/`total_pages`.
- Every gRPC endpoint also gets a sample request in `http/<service>.http` (IntelliJ HTTP Client `GRPC` requests to
  `{{grpc-host}}/pl.dworski.grpcwithspring.<Service>/<Method>`, JSON body with proto field names, request title
  `### Call <Service>/<Method> (select the environment)`). Add it together with the endpoint.

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
- MongoDB must run as a replica set, because transactions fail on a standalone `mongod`. Compose and tests run a
  single-node replica set `rs0` with authentication, which requires a `keyFile`: the container command generates a
  random keyfile, then `exec`s `docker-entrypoint.sh mongod --replSet rs0 --keyFile ...`. The entrypoint drops
  `--replSet`/`--keyFile` for its initial root-user run. `rs.initiate` with member `localhost:27017` runs afterwards: in
  compose from the healthcheck, in tests from `containerIsStarted`. The app connects with a single host and no replica
  set name, so the driver connects directly and the member host never has to be reachable from outside.
- MongoDB images: use `mongo:9.0` (compose and tests). `mongo:8.0` refuses to start on the dev machine's Linux kernel
  7.x (SERVER-121912).

## Testing

- Tests are written in Groovy with Spock (`src/test/groovy`). Spring Boot integration specs require a running Docker
  daemon (Testcontainers).
- `testLogging` in `build.gradle.kts` prints test stdout/stderr (Spring, Testcontainers logs), per-test results and full
  stack traces to the console. When nothing changed, Gradle marks `test` up-to-date and shows nothing. Use
  `./gradlew test --rerun` to run the tests again anyway.
- Domain services get plain unit specs without Spring or Docker (e.g. `CreateUserServiceSpec`): construct the service
  directly and mock its outbound ports with Spock `Mock()`. A mocked `TransactionRunner` is stubbed to run the action
  (`transactionRunner.inTransaction(_) >> { Supplier action -> action.get() }`, see `PlaceOrderServiceSpec`).
- Write conflicts are tested against the real Mongo container, never with mocks (`MongoTransactionRunnerSpec`,
  `OrderGrpcServiceSpec`). A "holder" transaction in another thread (`CompletableFuture.supplyAsync`, since Spring binds
  a transaction to its thread) decreases stock and waits on a `CountDownLatch`, keeping the change uncommitted. The
  latches are named after the event they signal (`holderDecreasedStock`, `holderMayCommit`). Release the holder in a
  `cleanup:` block, so it is released even when an assertion fails. Every latch/future wait has a 10 s timeout.
- Spring Boot integration specs use `@SpringBootTest`, `@ActiveProfiles('test')` and
  `@Import(MongoTestContainerConfiguration)`. That configuration starts a `GenericContainer` with `mongo:9.0` (as a
  single-node replica set, see Profiles and MongoDB) whose root user is created from the
  `spring.mongodb.username/password` of the `test` profile (single source of credentials), and registers the
  container's host/mapped port via a `DynamicPropertyRegistrar`. `MongoDBContainer` from
  `testcontainers-mongodb` is deliberately not used: it runs without authentication.
- Every `@SpringBootTest` spec also needs `@AutoConfigureTestGrpcTransport` (in-process transport, no port). Without it
  the context starts a Netty gRPC server on 9090 and fails with `BindException` whenever the app is already running
  locally. Specs with an identical annotation set share one cached context and one Mongo container. Each different
  `@ImportGrpcClients` set (`HelloGrpcServiceSpec`, `UserGrpcServiceSpec`, `ProductGrpcServiceSpec`,
  `OrderGrpcServiceSpec`) starts its own context and container.
- Clean up entity collections with `mongoTemplate.remove(new Query(), ...)`, never `dropCollection`. Indexes (e.g.
  unique `users.email`) are created only once, at context startup, and the context is shared, so dropping a collection
  silently removes its indexes for every spec that runs later.
- gRPC tests additionally use `@ImportGrpcClients(types = ...BlockingStub)` to inject client stubs.
- Spock evaluates `where:` before `setup()`, and it can reach only static and `@Shared` fields there. Data tables must
  not use entities created in `setup()`. Validation errors are raised before any database access, so tables of invalid
  requests use constants (e.g. `UNKNOWN_ID` in `OrderGrpcServiceSpec`), and cases that need real entities are separate
  feature methods.

## Commands

```bash
./gradlew build                     # compile (incl. protobuf codegen) + tests
docker compose up -d                # start local MongoDB (re-run after compose.yaml changes to recreate it)
./gradlew bootRun --args='--spring.profiles.active=local'   # run against local MongoDB
./gradlew test                      # all tests (Spock on JUnit Platform)
./gradlew generateProto             # regenerate Java sources from .proto files
```
