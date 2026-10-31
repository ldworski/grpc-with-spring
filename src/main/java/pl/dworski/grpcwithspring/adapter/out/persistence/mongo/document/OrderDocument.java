package pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import pl.dworski.grpcwithspring.domain.model.Order;

@Document("orders")
public record OrderDocument(@Id String id, @Indexed String userId, List<OrderItemDocument> items) {

    public static OrderDocument fromDomain(Order order) {
        return new OrderDocument(order.id(), order.userId(),
                order.items().stream().map(OrderItemDocument::fromDomain).toList());
    }

    public Order toDomain() {
        return new Order(id, userId, items.stream().map(OrderItemDocument::toDomain).toList());
    }

}
