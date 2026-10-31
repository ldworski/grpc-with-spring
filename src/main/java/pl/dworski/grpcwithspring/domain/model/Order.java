package pl.dworski.grpcwithspring.domain.model;

import java.util.List;
import java.util.Objects;

public record Order(String id, String userId, List<OrderItem> items) {

    public Order {
        Objects.requireNonNull(userId, "userId must not be null");
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        if (items.isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
    }

    public static Order create(String userId, List<OrderItem> items) {
        return new Order(null, userId, items);
    }

}
