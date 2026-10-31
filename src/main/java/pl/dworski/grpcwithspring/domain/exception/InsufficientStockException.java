package pl.dworski.grpcwithspring.domain.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productId, int requested, int available) {
        super("Product '%s' has %d item(s) in stock, %d requested".formatted(productId, available, requested));
    }

}
