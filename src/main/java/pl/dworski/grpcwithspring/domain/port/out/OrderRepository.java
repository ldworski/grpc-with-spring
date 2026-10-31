package pl.dworski.grpcwithspring.domain.port.out;

import java.util.List;
import java.util.Optional;
import pl.dworski.grpcwithspring.domain.model.Order;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(String id);

    List<Order> findByUserId(String userId);

}
