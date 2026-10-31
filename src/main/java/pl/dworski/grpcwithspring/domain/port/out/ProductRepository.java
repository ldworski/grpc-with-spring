package pl.dworski.grpcwithspring.domain.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import pl.dworski.grpcwithspring.domain.model.Product;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(String id);

    List<Product> findAllByIds(Collection<String> ids);

}
