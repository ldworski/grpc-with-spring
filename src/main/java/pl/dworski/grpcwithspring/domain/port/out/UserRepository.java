package pl.dworski.grpcwithspring.domain.port.out;

import java.util.Optional;
import pl.dworski.grpcwithspring.domain.model.User;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(String id);

    Optional<User> findByEmail(String email);

}
