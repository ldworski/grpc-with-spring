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
import pl.dworski.grpcwithspring.domain.model.OrderItem
import pl.dworski.grpcwithspring.domain.model.Product
import pl.dworski.grpcwithspring.domain.model.User
import pl.dworski.grpcwithspring.domain.port.out.OrderRepository
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository
import pl.dworski.grpcwithspring.domain.port.out.TransactionRunner
import pl.dworski.grpcwithspring.domain.port.out.UserRepository
import pl.dworski.grpcwithspring.proto.OrderServiceGrpc
import pl.dworski.grpcwithspring.proto.PlaceOrderRequest
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch

import static java.util.concurrent.TimeUnit.SECONDS

@SpringBootTest
@ActiveProfiles('test')
@Import(MongoTestContainerConfiguration)
@AutoConfigureTestGrpcTransport
@ImportGrpcClients(types = OrderServiceGrpc.OrderServiceBlockingStub)
class OrderGrpcServiceSpec extends Specification {

    static final String UNKNOWN_ID = '000000000000000000000000'

    @Autowired
    OrderServiceGrpc.OrderServiceBlockingStub orderStub

    @Autowired
    UserRepository userRepository

    @Autowired
    ProductRepository productRepository

    @Autowired
    OrderRepository orderRepository

    @Autowired
    TransactionRunner transactionRunner

    @Autowired
    MongoTemplate mongoTemplate

    User user

    Product keyboard

    def setup() {
        user = userRepository.save(User.create('luke@example.com', BigDecimal.TEN))
        keyboard = productRepository.save(Product.create('Keyboard', 5))
    }

    def cleanup() {
        ['users', 'products', 'orders'].each { mongoTemplate.remove(new Query(), it) }
    }

    def "places an order and returns it"() {
        given:
        def mouse = productRepository.save(Product.create('Mouse', 3))

        when:
        def response = orderStub.placeOrder(request(user.id(), [(keyboard.id()): 2, (mouse.id()): 3]))

        then:
        !response.order.id.isEmpty()
        response.order.userId == user.id()
        response.order.itemsList*.productId == [keyboard.id(), mouse.id()]
        response.order.itemsList*.quantity == [2, 3]

        and:
        orderRepository.findById(response.order.id).get().items() ==
                [new OrderItem(keyboard.id(), 2), new OrderItem(mouse.id(), 3)]
        productRepository.findById(keyboard.id()).get().stockQuantity() == 3
        productRepository.findById(mouse.id()).get().stockQuantity() == 0
    }

    def "returns NOT_FOUND for an unknown user"() {
        when:
        orderStub.placeOrder(request(UNKNOWN_ID, [(keyboard.id()): 1]))

        then:
        def e = thrown(StatusRuntimeException)
        e.status.code == Status.Code.NOT_FOUND
        e.status.description == "User with id '$UNKNOWN_ID' does not exist"
        productRepository.findById(keyboard.id()).get().stockQuantity() == 5
        mongoTemplate.count(new Query(), 'orders') == 0
    }

    def "returns NOT_FOUND and rolls back stock when a product does not exist"() {
        when:
        orderStub.placeOrder(request(user.id(), [(keyboard.id()): 2, (UNKNOWN_ID): 1]))

        then:
        def e = thrown(StatusRuntimeException)
        e.status.code == Status.Code.NOT_FOUND
        e.status.description == "Product with id '$UNKNOWN_ID' does not exist"
        productRepository.findById(keyboard.id()).get().stockQuantity() == 5
        mongoTemplate.count(new Query(), 'orders') == 0
    }

    def "returns FAILED_PRECONDITION and rolls back stock when a product has insufficient stock"() {
        given:
        def mouse = productRepository.save(Product.create('Mouse', 1))

        when:
        orderStub.placeOrder(request(user.id(), [(keyboard.id()): 2, (mouse.id()): 2]))

        then:
        def e = thrown(StatusRuntimeException)
        e.status.code == Status.Code.FAILED_PRECONDITION
        e.status.description == "Product '${mouse.id()}' has 1 item(s) in stock, 2 requested"

        and:
        productRepository.findById(keyboard.id()).get().stockQuantity() == 5
        productRepository.findById(mouse.id()).get().stockQuantity() == 1
        mongoTemplate.count(new Query(), 'orders') == 0
    }

    def "returns ABORTED when a concurrent transaction blocks the product during all retries"() {
        given:
        def holderDecreasedStock = new CountDownLatch(1)
        def holderMayCommit = new CountDownLatch(1)

        and: 'a holder transaction in another thread keeps a stock change of the keyboard uncommitted'
        def holderTransaction = CompletableFuture.supplyAsync {
            transactionRunner.inTransaction {
                productRepository.decreaseStockIfAvailable(keyboard.id(), 1)
                holderDecreasedStock.countDown()
                holderMayCommit.await(10, SECONDS)
            }
        }
        holderDecreasedStock.await(10, SECONDS)

        when:
        orderStub.placeOrder(request(user.id(), [(keyboard.id()): 1]))

        then:
        def e = thrown(StatusRuntimeException)
        e.status.code == Status.Code.ABORTED
        e.status.description == 'Transaction was aborted by concurrent modifications, please try again'
        mongoTemplate.count(new Query(), 'orders') == 0

        cleanup:
        holderMayCommit.countDown()
        holderTransaction.get(10, SECONDS)
    }

    def "returns INVALID_ARGUMENT for #description"() {
        when:
        orderStub.placeOrder(invalidRequest)

        then:
        def e = thrown(StatusRuntimeException)
        e.status.code == Status.Code.INVALID_ARGUMENT
        e.status.description == message
        mongoTemplate.count(new Query(), 'orders') == 0

        where:
        description              | invalidRequest                               || message
        'a missing user id'      | request('', [(UNKNOWN_ID): 1])               || 'userId must not be blank'
        'no items'               | request(UNKNOWN_ID, [:])                     || 'order must contain at least one item'
        'a missing product id'   | request(UNKNOWN_ID, ['': 1])                 || 'productId must not be blank'
        'a zero quantity'        | request(UNKNOWN_ID, [(UNKNOWN_ID): 0])       || 'quantity must be positive'
        'the same product twice' | requestWithDuplicate(UNKNOWN_ID, UNKNOWN_ID) || "product '$UNKNOWN_ID' is ordered more than once"
    }

    private static PlaceOrderRequest request(String userId, Map<String, Integer> quantitiesByProductId) {
        def builder = PlaceOrderRequest.newBuilder().setUserId(userId)
        quantitiesByProductId.each { productId, quantity ->
            builder.addItems(pl.dworski.grpcwithspring.proto.OrderItem.newBuilder()
                    .setProductId(productId)
                    .setQuantity(quantity))
        }
        builder.build()
    }

    private static PlaceOrderRequest requestWithDuplicate(String userId, String productId) {
        def item = pl.dworski.grpcwithspring.proto.OrderItem.newBuilder().setProductId(productId).setQuantity(1)
        PlaceOrderRequest.newBuilder().setUserId(userId).addItems(item).addItems(item).build()
    }

}
