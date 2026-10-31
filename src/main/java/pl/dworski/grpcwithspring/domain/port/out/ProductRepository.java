package pl.dworski.grpcwithspring.domain.port.out;

import pl.dworski.grpcwithspring.domain.model.Product;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(String id);

    List<Product> findAllByIds(Collection<String> ids);

}
