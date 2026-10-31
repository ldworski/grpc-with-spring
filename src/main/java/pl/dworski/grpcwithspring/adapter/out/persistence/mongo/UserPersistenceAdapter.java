package pl.dworski.grpcwithspring.adapter.out.persistence.mongo;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document.UserDocument;
import pl.dworski.grpcwithspring.domain.exception.UserAlreadyExistsException;
import pl.dworski.grpcwithspring.domain.model.User;
import pl.dworski.grpcwithspring.domain.port.out.UserRepository;

import java.util.Optional;

@Component
class UserPersistenceAdapter implements UserRepository {

    private final UserMongoRepository repository;

    UserPersistenceAdapter(UserMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        try {
            return repository.save(UserDocument.fromDomain(user)).toDomain();
        } catch (DuplicateKeyException e) {
            throw new UserAlreadyExistsException(user.email());
        }
    }

    @Override
    public Optional<User> findById(String id) {
        return repository.findById(id).map(UserDocument::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(UserDocument::toDomain);
    }

}
