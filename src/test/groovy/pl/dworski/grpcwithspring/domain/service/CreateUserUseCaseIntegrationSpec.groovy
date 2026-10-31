package pl.dworski.grpcwithspring.domain.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.ActiveProfiles
import pl.dworski.grpcwithspring.MongoTestContainerConfiguration
import pl.dworski.grpcwithspring.domain.exception.UserAlreadyExistsException
import pl.dworski.grpcwithspring.domain.port.in.CreateUserUseCase
import pl.dworski.grpcwithspring.domain.port.in.CreateUserUseCase.CreateUserCommand
import pl.dworski.grpcwithspring.domain.port.out.UserRepository
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles('test')
@Import(MongoTestContainerConfiguration)
@AutoConfigureTestGrpcTransport
class CreateUserUseCaseIntegrationSpec extends Specification {

    @Autowired
    CreateUserUseCase createUserUseCase

    @Autowired
    UserRepository userRepository

    @Autowired
    MongoTemplate mongoTemplate

    def cleanup() {
        mongoTemplate.remove(new Query(), 'users')
    }

    def "creates a user persisted in MongoDB"() {
        when:
        def created = createUserUseCase.createUser(new CreateUserCommand('luke@example.com', new BigDecimal('25.00')))

        then:
        created.id() != null
        userRepository.findByEmail('luke@example.com') == Optional.of(created)
    }

    def "does not create a second user with the same email"() {
        given:
        createUserUseCase.createUser(new CreateUserCommand('luke@example.com', BigDecimal.ONE))

        when:
        createUserUseCase.createUser(new CreateUserCommand('luke@example.com', BigDecimal.TEN))

        then:
        thrown(UserAlreadyExistsException)
        mongoTemplate.getCollection('users').countDocuments() == 1
    }

}
