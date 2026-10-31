package pl.dworski.grpcwithspring.domain.service;

import pl.dworski.grpcwithspring.domain.model.Product;
import pl.dworski.grpcwithspring.domain.port.in.CreateProductUseCase;
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository;

public class CreateProductService implements CreateProductUseCase {

    private final ProductRepository productRepository;

    public CreateProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product createProduct(CreateProductCommand command) {
        return productRepository.save(Product.create(command.name(), command.initialStockQuantity()));
    }

}
