package pl.dworski.grpcwithspring.domain.port.in;

import pl.dworski.grpcwithspring.domain.model.Order;
import pl.dworski.grpcwithspring.domain.model.OrderItem;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public interface PlaceOrderUseCase {

    Order placeOrder(PlaceOrderCommand command);

    record PlaceOrderCommand(String userId, List<OrderItem> items) {

        public PlaceOrderCommand {
            Objects.requireNonNull(userId, "userId must not be null");
            if (userId.isBlank()) {
                throw new IllegalArgumentException("userId must not be blank");
            }
            items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
            if (items.isEmpty()) {
                throw new IllegalArgumentException("order must contain at least one item");
            }
            var productIds = new HashSet<String>();
            for (var item : items) {
                if (!productIds.add(item.productId())) {
                    throw new IllegalArgumentException("product '%s' is ordered more than once".formatted(item.productId()));
                }
            }
        }

    }

}
