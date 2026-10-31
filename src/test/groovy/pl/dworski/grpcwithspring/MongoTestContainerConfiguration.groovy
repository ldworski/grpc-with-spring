package pl.dworski.grpcwithspring

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

@TestConfiguration
class MongoTestContainerConfiguration {

    static final int MONGO_PORT = 27017

    @Bean
    GenericContainer<?> mongoContainer(@Value('${spring.mongodb.username}') String username,
                                       @Value('${spring.mongodb.password}') String password) {
        new GenericContainer<>('mongo:9.0')
                .withExposedPorts(MONGO_PORT)
                .withEnv('MONGO_INITDB_ROOT_USERNAME', username)
                .withEnv('MONGO_INITDB_ROOT_PASSWORD', password)
        // the entrypoint starts mongod twice: once to create the root user, then for real
                .waitingFor(Wait.forLogMessage('.*Waiting for connections.*', 2))
    }

    @Bean
    DynamicPropertyRegistrar mongoContainerProperties(GenericContainer<?> mongoContainer) {
        { registry ->
            registry.add('spring.mongodb.host', mongoContainer::getHost)
            registry.add('spring.mongodb.port', () -> mongoContainer.getMappedPort(MONGO_PORT))
        } as DynamicPropertyRegistrar
    }

}
