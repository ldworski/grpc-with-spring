package pl.dworski.grpcwithspring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.dworski.grpcwithspring.domain.port.in.CreateProductUseCase;
import pl.dworski.grpcwithspring.domain.port.in.CreateUserUseCase;
import pl.dworski.grpcwithspring.domain.port.in.GetProductsUseCase;
import pl.dworski.grpcwithspring.domain.port.in.PlaceOrderUseCase;
import pl.dworski.grpcwithspring.domain.port.out.OrderRepository;
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository;
import pl.dworski.grpcwithspring.domain.port.out.TransactionRunner;
import pl.dworski.grpcwithspring.domain.port.out.UserRepository;
import pl.dworski.grpcwithspring.domain.service.CreateProductService;
import pl.dworski.grpcwithspring.domain.service.CreateUserService;
import pl.dworski.grpcwithspring.domain.service.GetProductsService;
import pl.dworski.grpcwithspring.domain.service.PlaceOrderService;

@Configuration
class DomainConfiguration {

    @Bean
    CreateProductUseCase createProductUseCase(ProductRepository productRepository) {
        return new CreateProductService(productRepository);
    }

    @Bean
    GetProductsUseCase getProductsUseCase(ProductRepository productRepository) {
        return new GetProductsService(productRepository);
    }

    @Bean
    CreateUserUseCase createUserUseCase(UserRepository userRepository) {
        return new CreateUserService(userRepository);
    }

    @Bean
    PlaceOrderUseCase placeOrderUseCase(UserRepository userRepository, ProductRepository productRepository,
                                        OrderRepository orderRepository, TransactionRunner transactionRunner) {
        return new PlaceOrderService(userRepository, productRepository, orderRepository, transactionRunner);
    }

}
