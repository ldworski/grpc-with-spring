package pl.dworski.grpcwithspring.domain.service;

import pl.dworski.grpcwithspring.domain.model.Product;
import pl.dworski.grpcwithspring.domain.model.paging.Page;
import pl.dworski.grpcwithspring.domain.port.in.GetProductsUseCase;
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository;

public class GetProductsService implements GetProductsUseCase {

    private final ProductRepository productRepository;

    public GetProductsService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Page<Product> getProducts(GetProductsQuery query) {
        return productRepository.findAll(query.pageQuery());
    }

}
