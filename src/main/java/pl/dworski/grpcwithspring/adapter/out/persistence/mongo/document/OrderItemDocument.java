package pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document;

import pl.dworski.grpcwithspring.domain.model.OrderItem;

public record OrderItemDocument(String productId, int quantity) {

    public static OrderItemDocument fromDomain(OrderItem item) {
        return new OrderItemDocument(item.productId(), item.quantity());
    }

    public OrderItem toDomain() {
        return new OrderItem(productId, quantity);
    }

}
