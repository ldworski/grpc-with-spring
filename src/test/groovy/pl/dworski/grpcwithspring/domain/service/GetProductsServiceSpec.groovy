package pl.dworski.grpcwithspring.domain.service

import pl.dworski.grpcwithspring.domain.model.Product
import pl.dworski.grpcwithspring.domain.model.paging.Page
import pl.dworski.grpcwithspring.domain.model.paging.PageQuery
import pl.dworski.grpcwithspring.domain.port.in.GetProductsUseCase.GetProductsQuery
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository
import spock.lang.Specification

class GetProductsServiceSpec extends Specification {

    ProductRepository productRepository = Mock()

    GetProductsService service = new GetProductsService(productRepository)

    def "returns the page of products from the repository"() {
        given:
        def page = new Page([new Product('product-1', 'Keyboard', 10), new Product('product-2', 'Mouse', 5)], 1, 2, 5)

        when:
        def result = service.getProducts(new GetProductsQuery(new PageQuery(1, 2)))

        then:
        1 * productRepository.findAll(new PageQuery(1, 2)) >> page
        result == page
    }

    def "uses the default page query when parameters are missing"() {
        given:
        def page = new Page<Product>([], 0, 20, 0)

        when:
        def result = service.getProducts(new GetProductsQuery(PageQuery.of(null, null)))

        then:
        1 * productRepository.findAll(new PageQuery(0, 20)) >> page
        result == page
    }

    def "caps the page size before querying the repository"() {
        when:
        service.getProducts(new GetProductsQuery(PageQuery.of(0, 100)))

        then:
        1 * productRepository.findAll(new PageQuery(0, 50)) >> new Page<Product>([], 0, 50, 0)
    }

    def "rejects a query without a page query"() {
        when:
        new GetProductsQuery(null)

        then:
        thrown(NullPointerException)
    }

}
