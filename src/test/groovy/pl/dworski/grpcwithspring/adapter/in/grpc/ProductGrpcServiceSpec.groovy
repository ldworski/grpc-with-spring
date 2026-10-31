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
import pl.dworski.grpcwithspring.domain.model.Product
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository
import pl.dworski.grpcwithspring.proto.CreateProductRequest
import pl.dworski.grpcwithspring.proto.GetProductsRequest
import pl.dworski.grpcwithspring.proto.ProductServiceGrpc
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles('test')
@Import(MongoTestContainerConfiguration)
@AutoConfigureTestGrpcTransport
@ImportGrpcClients(types = ProductServiceGrpc.ProductServiceBlockingStub)
class ProductGrpcServiceSpec extends Specification {

    @Autowired
    ProductServiceGrpc.ProductServiceBlockingStub productStub

    @Autowired
    ProductRepository productRepository

    @Autowired
    MongoTemplate mongoTemplate

    def cleanup() {
        mongoTemplate.remove(new Query(), 'products')
    }

    def "creates a product and returns it"() {
        when:
        def response = productStub.createProduct(request('Keyboard', 10))

        then:
        !response.product.id.isEmpty()
        response.product.name == 'Keyboard'
        response.product.stockQuantity == 10
        productRepository.findById(response.product.id).get() == new Product(response.product.id, 'Keyboard', 10)
    }

    def "creates a product with zero stock quantity"() {
        when:
        def response = productStub.createProduct(request('Mouse', 0))

        then:
        response.product.stockQuantity == 0
        productRepository.findById(response.product.id).isPresent()
    }

    def "returns INVALID_ARGUMENT for invalid input"() {
        when:
        productStub.createProduct(request(name, stockQuantity))

        then:
        def e = thrown(StatusRuntimeException)
        e.status.code == Status.Code.INVALID_ARGUMENT
        mongoTemplate.count(new Query(), 'products') == 0

        where:
        name       | stockQuantity
        ''         | 5
        '   '      | 5
        'Keyboard' | -1
    }

    def "returns INVALID_ARGUMENT when initial stock quantity is missing"() {
        when:
        productStub.createProduct(CreateProductRequest.newBuilder().setName('Keyboard').build())

        then:
        def e = thrown(StatusRuntimeException)
        e.status.code == Status.Code.INVALID_ARGUMENT
        e.status.description == 'initial_stock_quantity is required'
        mongoTemplate.count(new Query(), 'products') == 0
    }

    def "returns a page of products sorted by id"() {
        given:
        def saved = ['Keyboard', 'Mouse', 'Monitor']
                .collect { productRepository.save(Product.create(it, 1)) }
                .sort { it.id() }

        when:
        def response = productStub.getProducts(GetProductsRequest.newBuilder().setPage(1).setPageSize(2).build())

        then:
        response.productsList*.id == [saved[2].id()]
        response.productsList*.name == [saved[2].name()]
        response.page == 1
        response.pageSize == 2
        response.totalElements == 3
        response.totalPages == 2
    }

    def "uses default paging when parameters are missing"() {
        given:
        productRepository.save(Product.create('Keyboard', 10))

        when:
        def response = productStub.getProducts(GetProductsRequest.getDefaultInstance())

        then:
        response.productsCount == 1
        response.page == 0
        response.pageSize == 20
        response.totalElements == 1
        response.totalPages == 1
    }

    def "caps page size at 50"() {
        when:
        def response = productStub.getProducts(GetProductsRequest.newBuilder().setPageSize(100).build())

        then:
        response.pageSize == 50
    }

    def "returns INVALID_ARGUMENT for invalid paging parameters"() {
        when:
        productStub.getProducts(request)

        then:
        def e = thrown(StatusRuntimeException)
        e.status.code == Status.Code.INVALID_ARGUMENT
        e.status.description == message

        where:
        request                                                || message
        GetProductsRequest.newBuilder().setPage(-1).build()    || 'page must not be negative'
        GetProductsRequest.newBuilder().setPageSize(0).build() || 'size must be positive'
    }

    private static CreateProductRequest request(String name, int stockQuantity) {
        CreateProductRequest.newBuilder().setName(name).setInitialStockQuantity(stockQuantity).build()
    }

}
