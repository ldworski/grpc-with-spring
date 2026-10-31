package pl.dworski.grpcwithspring.domain.port.out;

import pl.dworski.grpcwithspring.domain.model.Product;
import pl.dworski.grpcwithspring.domain.model.paging.Page;
import pl.dworski.grpcwithspring.domain.model.paging.PageQuery;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(String id);

    List<Product> findAllByIds(Collection<String> ids);

    Page<Product> findAll(PageQuery pageQuery);

}
