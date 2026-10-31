package pl.dworski.grpcwithspring.adapter.out.persistence.mongo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document.ProductDocument;
import pl.dworski.grpcwithspring.domain.model.Product;
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository;

@Component
class ProductPersistenceAdapter implements ProductRepository {

    private final ProductMongoRepository repository;

    ProductPersistenceAdapter(ProductMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Product save(Product product) {
        return repository.save(ProductDocument.fromDomain(product)).toDomain();
    }

    @Override
    public Optional<Product> findById(String id) {
        return repository.findById(id).map(ProductDocument::toDomain);
    }

    @Override
    public List<Product> findAllByIds(Collection<String> ids) {
        return repository.findAllById(ids).stream().map(ProductDocument::toDomain).toList();
    }

}
