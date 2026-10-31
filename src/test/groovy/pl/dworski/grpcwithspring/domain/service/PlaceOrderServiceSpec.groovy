package pl.dworski.grpcwithspring.domain.service

import pl.dworski.grpcwithspring.domain.exception.InsufficientStockException
import pl.dworski.grpcwithspring.domain.exception.ProductNotFoundException
import pl.dworski.grpcwithspring.domain.exception.UserNotFoundException
import pl.dworski.grpcwithspring.domain.model.Order
import pl.dworski.grpcwithspring.domain.model.OrderItem
import pl.dworski.grpcwithspring.domain.model.Product
import pl.dworski.grpcwithspring.domain.model.User
import pl.dworski.grpcwithspring.domain.port.in.PlaceOrderUseCase.PlaceOrderCommand
import pl.dworski.grpcwithspring.domain.port.out.OrderRepository
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository
import pl.dworski.grpcwithspring.domain.port.out.TransactionRunner
import pl.dworski.grpcwithspring.domain.port.out.UserRepository
import spock.lang.Specification

import java.util.function.Supplier

class PlaceOrderServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    ProductRepository productRepository = Mock()
    OrderRepository orderRepository = Mock()
    TransactionRunner transactionRunner = Mock()

    PlaceOrderService service = new PlaceOrderService(userRepository, productRepository, orderRepository,
            transactionRunner)

    def setup() {
        transactionRunner.inTransaction(_) >> { Supplier action -> action.get() }
    }

    def "decreases stock of every product and saves the order"() {
        given:
        def items = [new OrderItem('product-1', 2), new OrderItem('product-2', 1)]
        userRepository.findById('user-1') >> Optional.of(new User('user-1', 'luke@example.com', BigDecimal.TEN))

        when:
        def placed = service.placeOrder(new PlaceOrderCommand('user-1', items))

        then:
        1 * productRepository.decreaseStockIfAvailable('product-1', 2) >> true
        1 * productRepository.decreaseStockIfAvailable('product-2', 1) >> true
        1 * orderRepository.save(Order.create('user-1', items)) >> new Order('order-1', 'user-1', items)
        placed == new Order('order-1', 'user-1', items)
    }

    def "accesses repositories only inside the transaction"() {
        given:
        def items = [new OrderItem('product-1', 1)]

        when:
        def placed = service.placeOrder(new PlaceOrderCommand('user-1', items))

        then:
        1 * transactionRunner.inTransaction(_) >> new Order('order-1', 'user-1', items)
        0 * userRepository._
        0 * productRepository._
        0 * orderRepository._
        placed == new Order('order-1', 'user-1', items)
    }

    def "rejects an order of an unknown user"() {
        given:
        userRepository.findById('user-1') >> Optional.empty()

        when:
        service.placeOrder(new PlaceOrderCommand('user-1', [new OrderItem('product-1', 1)]))

        then:
        thrown(UserNotFoundException)
        0 * productRepository.decreaseStockIfAvailable(*_)
        0 * orderRepository.save(_)
    }

    def "rejects an order when a product has insufficient stock"() {
        given:
        userRepository.findById('user-1') >> Optional.of(new User('user-1', 'luke@example.com', BigDecimal.TEN))
        productRepository.decreaseStockIfAvailable('product-1', 2) >> true
        productRepository.decreaseStockIfAvailable('product-2', 5) >> false
        productRepository.findById('product-2') >> Optional.of(new Product('product-2', 'Mouse', 3))

        when:
        service.placeOrder(new PlaceOrderCommand('user-1',
                [new OrderItem('product-1', 2), new OrderItem('product-2', 5)]))

        then:
        def exception = thrown(InsufficientStockException)
        exception.message == "Product 'product-2' has 3 item(s) in stock, 5 requested"
        0 * orderRepository.save(_)
    }

    def "rejects an order of an unknown product"() {
        given:
        userRepository.findById('user-1') >> Optional.of(new User('user-1', 'luke@example.com', BigDecimal.TEN))
        productRepository.decreaseStockIfAvailable('product-1', 1) >> false
        productRepository.findById('product-1') >> Optional.empty()

        when:
        service.placeOrder(new PlaceOrderCommand('user-1', [new OrderItem('product-1', 1)]))

        then:
        thrown(ProductNotFoundException)
        0 * orderRepository.save(_)
    }

    def "rejects a command with #description"() {
        when:
        new PlaceOrderCommand(userId, items)

        then:
        thrown(IllegalArgumentException)

        where:
        description              | userId   | items
        'a blank user id'        | '  '     | [new OrderItem('product-1', 1)]
        'no items'               | 'user-1' | []
        'the same product twice' | 'user-1' | [new OrderItem('product-1', 1), new OrderItem('product-1', 2)]
    }

}
