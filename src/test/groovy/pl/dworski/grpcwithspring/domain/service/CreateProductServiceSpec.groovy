package pl.dworski.grpcwithspring.domain.service

import pl.dworski.grpcwithspring.domain.model.Product
import pl.dworski.grpcwithspring.domain.port.in.CreateProductUseCase.CreateProductCommand
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository
import spock.lang.Specification

class CreateProductServiceSpec extends Specification {

    ProductRepository productRepository = Mock()

    CreateProductService service = new CreateProductService(productRepository)

    def "creates a new product with the given name and initial stock quantity"() {
        when:
        def created = service.createProduct(new CreateProductCommand('Keyboard', 10))

        then:
        1 * productRepository.save(Product.create('Keyboard', 10)) >> new Product('product-1', 'Keyboard', 10)
        created == new Product('product-1', 'Keyboard', 10)
    }

    def "creates a product with zero initial stock quantity"() {
        when:
        def created = service.createProduct(new CreateProductCommand('Mouse', 0))

        then:
        1 * productRepository.save(Product.create('Mouse', 0)) >> new Product('product-2', 'Mouse', 0)
        created == new Product('product-2', 'Mouse', 0)
    }

    def "rejects invalid product data: #description"() {
        when:
        service.createProduct(new CreateProductCommand(name, stockQuantity))

        then:
        thrown(IllegalArgumentException)
        0 * productRepository.save(_)

        where:
        description               | name       | stockQuantity
        'blank name'              | '   '      | 5
        'negative stock quantity' | 'Keyboard' | -1
    }

    def "rejects a command without a name"() {
        when:
        new CreateProductCommand(null, 5)

        then:
        thrown(NullPointerException)
    }

}
