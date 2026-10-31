package pl.dworski.grpcwithspring.adapter.out.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document.OrderDocument;

import java.util.List;

interface OrderMongoRepository extends MongoRepository<OrderDocument, String> {

    List<OrderDocument> findByUserId(String userId);

}
