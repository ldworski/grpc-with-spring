package pl.dworski.grpcwithspring

import com.github.dockerjava.api.command.InspectContainerResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

@TestConfiguration
class MongoTestContainerConfiguration {

    static final int MONGO_PORT = 27017

    static final String START_REPLICA_SET_MEMBER = '''\
        head -c 756 /dev/urandom | base64 -w 0 > /data/keyfile \
        && chown mongodb:mongodb /data/keyfile && chmod 400 /data/keyfile \
        && exec docker-entrypoint.sh mongod --replSet rs0 --keyFile /data/keyfile'''.stripIndent()

    static final String INITIATE_REPLICA_SET = '''\
        rs.initiate({_id: 'rs0', members: [{_id: 0, host: 'localhost:27017'}]});
        while (!db.hello().isWritablePrimary) { sleep(100) }'''.stripIndent()

    @Bean
    GenericContainer<?> mongoContainer(@Value('${spring.mongodb.username}') String username,
                                       @Value('${spring.mongodb.password}') String password) {
        new ReplicaSetMongoContainer(username, password)
                .withExposedPorts(MONGO_PORT)
                .withEnv('MONGO_INITDB_ROOT_USERNAME', username)
                .withEnv('MONGO_INITDB_ROOT_PASSWORD', password)
                .withCommand('bash', '-c', START_REPLICA_SET_MEMBER)
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

    static class ReplicaSetMongoContainer extends GenericContainer<ReplicaSetMongoContainer> {

        private final String username
        private final String password

        ReplicaSetMongoContainer(String username, String password) {
            super('mongo:9.0')
            this.username = username
            this.password = password
        }

        @Override
        protected void containerIsStarted(InspectContainerResponse containerInfo) {
            def result = execInContainer('mongosh', '--quiet', '-u', username, '-p', password,
                    '--authenticationDatabase', 'admin', '--eval', INITIATE_REPLICA_SET)
            if (result.exitCode != 0) {
                throw new IllegalStateException("Replica set initiation failed: ${result.stdout} ${result.stderr}")
            }
        }

    }

}
