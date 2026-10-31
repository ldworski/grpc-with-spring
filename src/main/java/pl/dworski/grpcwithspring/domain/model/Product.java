package pl.dworski.grpcwithspring.domain.model;

import java.util.Objects;

public record Product(String id, String name, int stockQuantity) {

    public Product {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("stockQuantity must not be negative");
        }
    }

    public static Product create(String name, int stockQuantity) {
        return new Product(null, name, stockQuantity);
    }

}
