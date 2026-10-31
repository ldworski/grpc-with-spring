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
import pl.dworski.grpcwithspring.domain.model.Product
import pl.dworski.grpcwithspring.domain.model.paging.PageQuery
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles('test')
@Import(MongoTestContainerConfiguration)
@AutoConfigureTestGrpcTransport
class ProductPersistenceAdapterSpec extends Specification {

    @Autowired
    ProductRepository productRepository

    @Autowired
    MongoTemplate mongoTemplate

    def cleanup() {
        mongoTemplate.remove(new Query(), ProductDocument)
    }

    def "saves a new product and assigns an id"() {
        when:
        def saved = productRepository.save(Product.create('Keyboard', 10))

        then:
        saved.id() != null
        productRepository.findById(saved.id()) == Optional.of(new Product(saved.id(), 'Keyboard', 10))
    }

    def "updates stock of an existing product instead of inserting a new one"() {
        given:
        def saved = productRepository.save(Product.create('Mouse', 5))

        when:
        productRepository.save(new Product(saved.id(), 'Mouse', 2))

        then:
        productRepository.findById(saved.id()).get().stockQuantity() == 2
        mongoTemplate.count(new Query(), ProductDocument) == 1
    }

    def "finds products by ids"() {
        given:
        def keyboard = productRepository.save(Product.create('Keyboard', 10))
        def mouse = productRepository.save(Product.create('Mouse', 5))
        productRepository.save(Product.create('Monitor', 3))

        expect:
        productRepository.findAllByIds([keyboard.id(), mouse.id()]) as Set == [keyboard, mouse] as Set
    }

    def "finds a page of products sorted by id"() {
        given:
        def saved = ['Keyboard', 'Mouse', 'Monitor']
                .collect { productRepository.save(Product.create(it, 1)) }
                .sort { it.id() }

        when:
        def firstPage = productRepository.findAll(new PageQuery(0, 2))
        def secondPage = productRepository.findAll(new PageQuery(1, 2))

        then:
        firstPage.content() == saved[0..1]
        firstPage.page() == 0
        firstPage.size() == 2
        firstPage.totalElements() == 3
        firstPage.totalPages() == 2

        and:
        secondPage.content() == [saved[2]]
        secondPage.page() == 1
    }

    def "returns an empty page when page is beyond the last one"() {
        given:
        productRepository.save(Product.create('Keyboard', 10))

        when:
        def page = productRepository.findAll(new PageQuery(1, 20))

        then:
        page.content().isEmpty()
        page.totalElements() == 1
        page.totalPages() == 1
    }

    def "returns an empty page when there are no products"() {
        when:
        def page = productRepository.findAll(PageQuery.of(null, null))

        then:
        page.content().isEmpty()
        page.totalElements() == 0
        page.totalPages() == 0
    }

    def "returns empty for unknown id"() {
        expect:
        productRepository.findById('000000000000000000000000').isEmpty()
    }

}
