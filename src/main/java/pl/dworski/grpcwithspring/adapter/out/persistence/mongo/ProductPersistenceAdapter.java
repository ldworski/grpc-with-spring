package pl.dworski.grpcwithspring.adapter.out.persistence.mongo;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document.ProductDocument;
import pl.dworski.grpcwithspring.domain.model.Product;
import pl.dworski.grpcwithspring.domain.model.paging.Page;
import pl.dworski.grpcwithspring.domain.model.paging.PageQuery;
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
class ProductPersistenceAdapter implements ProductRepository {

    private final ProductMongoRepository repository;
    private final MongoTemplate mongoTemplate;

    ProductPersistenceAdapter(ProductMongoRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
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

    @Override
    public Page<Product> findAll(PageQuery pageQuery) {
        var result = repository.findAll(PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by("id")));
        return new Page<>(result.map(ProductDocument::toDomain).getContent(), pageQuery.page(), pageQuery.size(),
                result.getTotalElements());
    }

    @Override
    public boolean decreaseStockIfAvailable(String productId, int quantity) {
        var query = Query.query(where("id").is(productId).and("stockQuantity").gte(quantity));
        var update = new Update().inc("stockQuantity", -quantity);
        return mongoTemplate.updateFirst(query, update, ProductDocument.class).getModifiedCount() == 1;
    }

}
