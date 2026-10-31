package pl.dworski.grpcwithspring.domain.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.ActiveProfiles
import pl.dworski.grpcwithspring.MongoTestContainerConfiguration
import pl.dworski.grpcwithspring.domain.exception.InsufficientStockException
import pl.dworski.grpcwithspring.domain.exception.ProductNotFoundException
import pl.dworski.grpcwithspring.domain.exception.UserNotFoundException
import pl.dworski.grpcwithspring.domain.model.OrderItem
import pl.dworski.grpcwithspring.domain.model.Product
import pl.dworski.grpcwithspring.domain.model.User
import pl.dworski.grpcwithspring.domain.port.in.PlaceOrderUseCase
import pl.dworski.grpcwithspring.domain.port.in.PlaceOrderUseCase.PlaceOrderCommand
import pl.dworski.grpcwithspring.domain.port.out.OrderRepository
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository
import pl.dworski.grpcwithspring.domain.port.out.UserRepository
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles('test')
@Import(MongoTestContainerConfiguration)
@AutoConfigureTestGrpcTransport
class PlaceOrderUseCaseIntegrationSpec extends Specification {

    @Autowired
    PlaceOrderUseCase placeOrderUseCase

    @Autowired
    UserRepository userRepository

    @Autowired
    ProductRepository productRepository

    @Autowired
    OrderRepository orderRepository

    @Autowired
    MongoTemplate mongoTemplate

    User user

    def setup() {
        user = userRepository.save(User.create('luke@example.com', BigDecimal.TEN))
    }

    def cleanup() {
        ['users', 'products', 'orders'].each { mongoTemplate.remove(new Query(), it) }
    }

    def "places an order and decreases stock of ordered products"() {
        given:
        def keyboard = productRepository.save(Product.create('Keyboard', 10))
        def mouse = productRepository.save(Product.create('Mouse', 1))
        def items = [new OrderItem(keyboard.id(), 3), new OrderItem(mouse.id(), 1)]

        when:
        def placed = placeOrderUseCase.placeOrder(new PlaceOrderCommand(user.id(), items))

        then:
        placed.id() != null
        orderRepository.findById(placed.id()) == Optional.of(placed)
        placed.items() == items
        productRepository.findById(keyboard.id()).get().stockQuantity() == 7
        productRepository.findById(mouse.id()).get().stockQuantity() == 0
    }

    def "rolls back stock already decreased when a later product has insufficient stock"() {
        given:
        def keyboard = productRepository.save(Product.create('Keyboard', 10))
        def mouse = productRepository.save(Product.create('Mouse', 1))

        when:
        placeOrderUseCase.placeOrder(new PlaceOrderCommand(user.id(),
                [new OrderItem(keyboard.id(), 3), new OrderItem(mouse.id(), 2)]))

        then:
        thrown(InsufficientStockException)
        productRepository.findById(keyboard.id()).get().stockQuantity() == 10
        productRepository.findById(mouse.id()).get().stockQuantity() == 1
        mongoTemplate.getCollection('orders').countDocuments() == 0
    }

    def "rolls back stock already decreased when a later product does not exist"() {
        given:
        def keyboard = productRepository.save(Product.create('Keyboard', 10))

        when:
        placeOrderUseCase.placeOrder(new PlaceOrderCommand(user.id(),
                [new OrderItem(keyboard.id(), 3), new OrderItem('000000000000000000000000', 1)]))

        then:
        thrown(ProductNotFoundException)
        productRepository.findById(keyboard.id()).get().stockQuantity() == 10
        mongoTemplate.getCollection('orders').countDocuments() == 0
    }

    def "rejects an order of an unknown user"() {
        given:
        def keyboard = productRepository.save(Product.create('Keyboard', 10))

        when:
        placeOrderUseCase.placeOrder(new PlaceOrderCommand('000000000000000000000000',
                [new OrderItem(keyboard.id(), 1)]))

        then:
        thrown(UserNotFoundException)
        productRepository.findById(keyboard.id()).get().stockQuantity() == 10
        mongoTemplate.getCollection('orders').countDocuments() == 0
    }

}
