package pl.dworski.grpcwithspring.adapter.out.persistence.mongo

import org.bson.types.Decimal128
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.ActiveProfiles
import pl.dworski.grpcwithspring.MongoTestContainerConfiguration
import pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document.UserDocument
import pl.dworski.grpcwithspring.domain.exception.UserAlreadyExistsException
import pl.dworski.grpcwithspring.domain.model.User
import pl.dworski.grpcwithspring.domain.port.out.UserRepository
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles('test')
@Import(MongoTestContainerConfiguration)
@AutoConfigureTestGrpcTransport
class UserPersistenceAdapterSpec extends Specification {

    @Autowired
    UserRepository userRepository

    @Autowired
    MongoTemplate mongoTemplate

    def cleanup() {
        mongoTemplate.remove(new Query(), UserDocument)
    }

    def "saves a user and finds it by id and email"() {
        when:
        def saved = userRepository.save(User.create('luke@example.com', new BigDecimal('100.50')))

        then:
        saved.id() != null
        userRepository.findById(saved.id()) == Optional.of(saved)
        userRepository.findByEmail('luke@example.com') == Optional.of(saved)
    }

    def "stores balance as Decimal128 without losing precision"() {
        given:
        def saved = userRepository.save(User.create('anna@example.com', new BigDecimal('0.10')))

        when:
        def raw = mongoTemplate.getCollection('users').find().first()

        then:
        raw.get('balance') instanceof Decimal128
        userRepository.findById(saved.id()).get().balance() == new BigDecimal('0.10')
    }

    def "translates a duplicate email into the domain exception"() {
        given:
        userRepository.save(User.create('dup@example.com', BigDecimal.ONE))

        when:
        userRepository.save(User.create('dup@example.com', BigDecimal.TEN))

        then:
        def e = thrown(UserAlreadyExistsException)
        e.message.contains('dup@example.com')
    }

}
