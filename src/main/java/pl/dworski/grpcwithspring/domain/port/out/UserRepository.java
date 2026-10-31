package pl.dworski.grpcwithspring.domain.port.out;

import pl.dworski.grpcwithspring.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(String id);

    Optional<User> findByEmail(String email);

}
