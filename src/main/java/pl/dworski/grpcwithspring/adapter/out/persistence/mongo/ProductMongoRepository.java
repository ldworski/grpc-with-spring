package pl.dworski.grpcwithspring.adapter.out.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document.ProductDocument;

interface ProductMongoRepository extends MongoRepository<ProductDocument, String> {
}
