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
import pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document.OrderDocument
import pl.dworski.grpcwithspring.domain.model.Order
import pl.dworski.grpcwithspring.domain.model.OrderItem
import pl.dworski.grpcwithspring.domain.port.out.OrderRepository
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles('test')
@Import(MongoTestContainerConfiguration)
@AutoConfigureTestGrpcTransport
class OrderPersistenceAdapterSpec extends Specification {

    @Autowired
    OrderRepository orderRepository

    @Autowired
    MongoTemplate mongoTemplate

    def cleanup() {
        mongoTemplate.remove(new Query(), OrderDocument)
    }

    def "saves an order with its items embedded in a single document"() {
        given:
        def items = [new OrderItem('product-1', 2), new OrderItem('product-2', 1)]

        when:
        def saved = orderRepository.save(Order.create('user-1', items))

        then:
        saved.id() != null
        orderRepository.findById(saved.id()) == Optional.of(new Order(saved.id(), 'user-1', items))
        mongoTemplate.getCollection('orders').find().first().getList('items', Document)*.productId ==
                ['product-1', 'product-2']
    }

    def "finds orders of a given user"() {
        given:
        def first = orderRepository.save(Order.create('user-1', [new OrderItem('product-1', 1)]))
        def second = orderRepository.save(Order.create('user-1', [new OrderItem('product-2', 3)]))
        orderRepository.save(Order.create('user-2', [new OrderItem('product-1', 1)]))

        expect:
        orderRepository.findByUserId('user-1') as Set == [first, second] as Set
    }

}
