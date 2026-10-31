package pl.dworski.grpcwithspring.domain.port.out;

import pl.dworski.grpcwithspring.domain.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(String id);

    List<Order> findByUserId(String userId);

}
