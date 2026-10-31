package pl.dworski.grpcwithspring.domain.service

import pl.dworski.grpcwithspring.domain.exception.UserAlreadyExistsException
import pl.dworski.grpcwithspring.domain.model.User
import pl.dworski.grpcwithspring.domain.port.in.CreateUserUseCase.CreateUserCommand
import pl.dworski.grpcwithspring.domain.port.out.UserRepository
import spock.lang.Specification

class CreateUserServiceSpec extends Specification {

    UserRepository userRepository = Mock()

    CreateUserService service = new CreateUserService(userRepository)

    def "creates a new user with the given email and initial balance"() {
        given:
        userRepository.findByEmail('luke@example.com') >> Optional.empty()

        when:
        def created = service.createUser(new CreateUserCommand('luke@example.com', new BigDecimal('50.00')))

        then:
        1 * userRepository.save(User.create('luke@example.com', new BigDecimal('50.00'))) >>
                new User('user-1', 'luke@example.com', new BigDecimal('50.00'))
        created == new User('user-1', 'luke@example.com', new BigDecimal('50.00'))
    }

    def "rejects an email that is already taken"() {
        given:
        userRepository.findByEmail('luke@example.com') >> Optional.of(new User('user-1', 'luke@example.com', BigDecimal.ONE))

        when:
        service.createUser(new CreateUserCommand('luke@example.com', BigDecimal.TEN))

        then:
        thrown(UserAlreadyExistsException)
        0 * userRepository.save(_)
    }

    def "rejects a negative initial balance"() {
        given:
        userRepository.findByEmail(_) >> Optional.empty()

        when:
        service.createUser(new CreateUserCommand('luke@example.com', new BigDecimal('-1')))

        then:
        thrown(IllegalArgumentException)
        0 * userRepository.save(_)
    }

}
