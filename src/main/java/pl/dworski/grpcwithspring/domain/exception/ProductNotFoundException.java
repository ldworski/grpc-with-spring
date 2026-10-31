package pl.dworski.grpcwithspring.domain.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String productId) {
        super("Product with id '%s' does not exist".formatted(productId));
    }

}
