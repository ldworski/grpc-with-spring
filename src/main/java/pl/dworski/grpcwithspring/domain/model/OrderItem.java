package pl.dworski.grpcwithspring.domain.model;

import java.util.Objects;

public record OrderItem(String productId, int quantity) {

    public OrderItem {
        Objects.requireNonNull(productId, "productId must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

}
