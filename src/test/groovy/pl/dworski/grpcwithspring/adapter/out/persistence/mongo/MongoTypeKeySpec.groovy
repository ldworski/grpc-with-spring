package pl.dworski.grpcwithspring.adapter.out.persistence.mongo

import org.bson.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.ActiveProfiles
import pl.dworski.grpcwithspring.MongoTestContainerConfiguration
import pl.dworski.grpcwithspring.domain.model.Order
import pl.dworski.grpcwithspring.domain.model.OrderItem
import pl.dworski.grpcwithspring.domain.model.Product
import pl.dworski.grpcwithspring.domain.model.User
import pl.dworski.grpcwithspring.domain.port.out.OrderRepository
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository
import pl.dworski.grpcwithspring.domain.port.out.UserRepository
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles('test')
@Import(MongoTestContainerConfiguration)
@AutoConfigureTestGrpcTransport
class MongoTypeKeySpec extends Specification {

    @Autowired
    UserRepository userRepository

    @Autowired
    ProductRepository productRepository

    @Autowired
    OrderRepository orderRepository

    @Autowired
    MongoTemplate mongoTemplate

    def cleanup() {
        ['users', 'products', 'orders'].each { mongoTemplate.remove(new Query(), it) }
    }

    def "does not store the _class type key in users"() {
        when:
        userRepository.save(User.create('luke@example.com', BigDecimal.TEN))

        then:
        !firstDocument('users').containsKey('_class')
    }

    def "does not store the _class type key in products"() {
        when:
        productRepository.save(Product.create('Keyboard', 3))

        then:
        !firstDocument('products').containsKey('_class')
    }

    def "does not store the _class type key in orders"() {
        when:
        orderRepository.save(Order.create('user-1', [new OrderItem('product-1', 2)]))

        then:
        !firstDocument('orders').containsKey('_class')
    }

    def "does not store the _class type key in embedded order items"() {
        given:
        orderRepository.save(Order.create('user-1', [new OrderItem('product-1', 2)]))

        when:
        def item = firstDocument('orders').getList('items', Document).first()

        then:
        !item.containsKey('_class')
    }

    private Document firstDocument(String collection) {
        def document = mongoTemplate.getCollection(collection).find().first()
        assert document != null
        document
    }

}
