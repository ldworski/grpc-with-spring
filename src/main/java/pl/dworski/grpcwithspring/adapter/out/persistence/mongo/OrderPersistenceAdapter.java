package pl.dworski.grpcwithspring.adapter.out.persistence.mongo;

import org.springframework.stereotype.Component;
import pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document.OrderDocument;
import pl.dworski.grpcwithspring.domain.model.Order;
import pl.dworski.grpcwithspring.domain.port.out.OrderRepository;

import java.util.List;
import java.util.Optional;

@Component
class OrderPersistenceAdapter implements OrderRepository {

    private final OrderMongoRepository repository;

    OrderPersistenceAdapter(OrderMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Order save(Order order) {
        return repository.save(OrderDocument.fromDomain(order)).toDomain();
    }

    @Override
    public Optional<Order> findById(String id) {
        return repository.findById(id).map(OrderDocument::toDomain);
    }

    @Override
    public List<Order> findByUserId(String userId) {
        return repository.findByUserId(userId).stream().map(OrderDocument::toDomain).toList();
    }

}
