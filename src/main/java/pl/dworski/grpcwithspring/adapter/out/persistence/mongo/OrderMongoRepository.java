package pl.dworski.grpcwithspring.adapter.out.persistence.mongo;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document.OrderDocument;

interface OrderMongoRepository extends MongoRepository<OrderDocument, String> {

    List<OrderDocument> findByUserId(String userId);

}
