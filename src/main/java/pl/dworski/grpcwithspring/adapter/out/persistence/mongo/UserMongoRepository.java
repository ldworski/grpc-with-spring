package pl.dworski.grpcwithspring.adapter.out.persistence.mongo;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document.UserDocument;

interface UserMongoRepository extends MongoRepository<UserDocument, String> {

    Optional<UserDocument> findByEmail(String email);

}
