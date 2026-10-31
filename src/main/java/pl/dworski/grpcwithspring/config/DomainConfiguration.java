package pl.dworski.grpcwithspring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.dworski.grpcwithspring.domain.port.in.CreateProductUseCase;
import pl.dworski.grpcwithspring.domain.port.in.CreateUserUseCase;
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository;
import pl.dworski.grpcwithspring.domain.port.out.UserRepository;
import pl.dworski.grpcwithspring.domain.service.CreateProductService;
import pl.dworski.grpcwithspring.domain.service.CreateUserService;

@Configuration
class DomainConfiguration {

    @Bean
    CreateProductUseCase createProductUseCase(ProductRepository productRepository) {
        return new CreateProductService(productRepository);
    }

    @Bean
    CreateUserUseCase createUserUseCase(UserRepository userRepository) {
        return new CreateUserService(userRepository);
    }

}
