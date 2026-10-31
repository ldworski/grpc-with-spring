package pl.dworski.grpcwithspring.adapter.in.grpc

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.grpc.client.ImportGrpcClients
import org.springframework.test.context.ActiveProfiles
import pl.dworski.grpcwithspring.MongoTestContainerConfiguration
import pl.dworski.grpcwithspring.domain.port.out.UserRepository
import pl.dworski.grpcwithspring.proto.CreateUserRequest
import pl.dworski.grpcwithspring.proto.UserServiceGrpc
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles('test')
@Import(MongoTestContainerConfiguration)
@AutoConfigureTestGrpcTransport
@ImportGrpcClients(types = UserServiceGrpc.UserServiceBlockingStub)
class UserGrpcServiceSpec extends Specification {

    @Autowired
    UserServiceGrpc.UserServiceBlockingStub userStub

    @Autowired
    UserRepository userRepository

    @Autowired
    MongoTemplate mongoTemplate

    def cleanup() {
        mongoTemplate.remove(new Query(), 'users')
    }

    def "creates a user and returns it"() {
        when:
        def response = userStub.createUser(request('luke@example.com', '100.50'))

        then:
        !response.user.id.isEmpty()
        response.user.email == 'luke@example.com'
        response.user.balance == '100.50'
        userRepository.findById(response.user.id).get().balance() == new BigDecimal('100.50')
    }

    def "returns ALREADY_EXISTS for a taken email"() {
        given:
        userStub.createUser(request('luke@example.com', '1'))

        when:
        userStub.createUser(request('luke@example.com', '2'))

        then:
        def e = thrown(StatusRuntimeException)
        e.status.code == Status.Code.ALREADY_EXISTS
    }

    def "returns INVALID_ARGUMENT for invalid input"() {
        when:
        userStub.createUser(request(email, balance))

        then:
        def e = thrown(StatusRuntimeException)
        e.status.code == Status.Code.INVALID_ARGUMENT

        where:
        email              | balance
        ''                 | '10'
        'luke@example.com' | '-1'
        'luke@example.com' | 'abc'
        'luke@example.com' | ''
    }

    private static CreateUserRequest request(String email, String balance) {
        CreateUserRequest.newBuilder().setEmail(email).setInitialBalance(balance).build()
    }

}
