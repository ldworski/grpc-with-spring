package pl.dworski.grpcwithspring.domain.model;

import java.util.Objects;

public record OrderItem(String productId, int quantity) {

    public OrderItem {
        Objects.requireNonNull(productId, "productId must not be null");
        if (productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

}
