package pl.dworski.grpcwithspring.adapter.out.persistence.mongo

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.ActiveProfiles
import pl.dworski.grpcwithspring.MongoTestContainerConfiguration
import pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document.ProductDocument
import pl.dworski.grpcwithspring.domain.exception.InsufficientStockException
import pl.dworski.grpcwithspring.domain.exception.TransactionConflictException
import pl.dworski.grpcwithspring.domain.model.Product
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository
import pl.dworski.grpcwithspring.domain.port.out.TransactionRunner
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

import static java.util.concurrent.TimeUnit.SECONDS

@SpringBootTest
@ActiveProfiles('test')
@Import(MongoTestContainerConfiguration)
@AutoConfigureTestGrpcTransport
class MongoTransactionRunnerSpec extends Specification {

    @Autowired
    TransactionRunner transactionRunner

    @Autowired
    ProductRepository productRepository

    @Autowired
    MongoTemplate mongoTemplate

    def cleanup() {
        mongoTemplate.remove(new Query(), ProductDocument)
    }

    def "retries a transaction aborted by a write conflict"() {
        given:
        def product = productRepository.save(Product.create('Keyboard', 10))
        def holderDecreasedStock = new CountDownLatch(1)
        def holderMayCommit = new CountDownLatch(1)
        def attempts = new AtomicInteger()

        and: 'a holder transaction in another thread decreases the stock and keeps the change uncommitted'
        def holderTransaction = holdUncommittedStockDecrease(product.id(), holderDecreasedStock, holderMayCommit)
        holderDecreasedStock.await(10, SECONDS)

        when: 'the first attempt conflicts with the holder, the second one lets the holder commit first'
        def decreased = transactionRunner.inTransaction {
            if (attempts.incrementAndGet() == 2) {
                holderMayCommit.countDown()
                holderTransaction.get(10, SECONDS)
            }
            productRepository.decreaseStockIfAvailable(product.id(), 1)
        }

        then:
        decreased
        attempts.get() == 2
        productRepository.findById(product.id()).get().stockQuantity() == 8
    }

    def "gives up with a domain exception when write conflicts persist after all retries"() {
        given:
        def product = productRepository.save(Product.create('Keyboard', 10))
        def holderDecreasedStock = new CountDownLatch(1)
        def holderMayCommit = new CountDownLatch(1)
        def attempts = new AtomicInteger()

        and: 'a holder transaction keeps the stock change uncommitted during every attempt'
        def holderTransaction = holdUncommittedStockDecrease(product.id(), holderDecreasedStock, holderMayCommit)
        holderDecreasedStock.await(10, SECONDS)

        when:
        transactionRunner.inTransaction {
            attempts.incrementAndGet()
            productRepository.decreaseStockIfAvailable(product.id(), 1)
        }

        then:
        thrown(TransactionConflictException)
        attempts.get() == 5
        productRepository.findById(product.id()).get().stockQuantity() == 10

        cleanup:
        holderMayCommit.countDown()
        holderTransaction.get(10, SECONDS)
    }

    def "does not retry a transaction that fails with a domain exception"() {
        given:
        def attempts = new AtomicInteger()

        when:
        transactionRunner.inTransaction {
            attempts.incrementAndGet()
            throw new InsufficientStockException('product-1', 2, 1)
        }

        then:
        thrown(InsufficientStockException)
        attempts.get() == 1
    }

    private CompletableFuture<Boolean> holdUncommittedStockDecrease(String productId,
                                                                    CountDownLatch holderDecreasedStock,
                                                                    CountDownLatch holderMayCommit) {
        CompletableFuture.supplyAsync {
            transactionRunner.inTransaction {
                productRepository.decreaseStockIfAvailable(productId, 1)
                holderDecreasedStock.countDown()
                holderMayCommit.await(10, SECONDS)
            }
        }
    }

}
