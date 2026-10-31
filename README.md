# grpc-with-spring

An order system (users order products) built with Spring Boot, exposed via gRPC and backed by MongoDB.

## Features

| Service          | Method          | Description                                                       |
|------------------|-----------------|-------------------------------------------------------------------|
| `UserService`    | `CreateUser`    | Creates a user with a unique email and an initial balance         |
| `ProductService` | `CreateProduct` | Creates a product with a name and an initial stock quantity       |
| `ProductService` | `GetProducts`   | Returns a page of products (default page size 20, at most 50)     |
| `OrderService`   | `PlaceOrder`    | Places an order and decreases stock atomically in one transaction |

The service definitions are in [`src/main/proto`](src/main/proto).

## Tech stack

- Java 25, Gradle (Kotlin DSL)
- Spring Boot 4.1 with Spring gRPC
- MongoDB 9.0 (run as a replica set, which transactions require), Spring Data MongoDB
- Protocol Buffers
- Spock and Testcontainers for tests

## Architecture

The application follows hexagonal architecture (ports and adapters):

- `domain`: the domain model, use cases (inbound ports), outbound ports and their implementations. No Spring or MongoDB
  dependencies.
- `adapter.in.grpc`: gRPC endpoints that call the use cases and map domain exceptions to gRPC statuses.
- `adapter.out.persistence.mongo`: the MongoDB implementation of the outbound ports, including transactions that are
  retried on write conflicts.

## Getting started

Requirements: JDK 25 (or let the Gradle toolchain provision it) and Docker.

```bash
docker compose up -d                                          # start a local MongoDB
./gradlew bootRun --args='--spring.profiles.active=local'     # run the app against it
```

The gRPC server listens on `localhost:9090`. Sample requests for every endpoint are in [`http`](http), for the
IntelliJ HTTP Client (select the `local` environment).

Without a profile the app uses the `prod` profile and reads the MongoDB connection from the environment variables
`MONGODB_HOST`, `MONGODB_PORT`, `MONGODB_DATABASE`, `MONGODB_USERNAME`, `MONGODB_PASSWORD` and
`MONGODB_AUTH_DATABASE`.

## Tests

```bash
./gradlew test
```

Integration tests start MongoDB in a container with Testcontainers, so they need a running Docker daemon.

## License

Licensed under the [Apache License 2.0](LICENSE).
