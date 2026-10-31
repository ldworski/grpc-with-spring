package pl.dworski.grpcwithspring.adapter.out.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document.UserDocument;

import java.util.Optional;

interface UserMongoRepository extends MongoRepository<UserDocument, String> {

    Optional<UserDocument> findByEmail(String email);

}
