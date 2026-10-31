package pl.dworski.grpcwithspring.domain.port.in;

import java.math.BigDecimal;
import java.util.Objects;
import pl.dworski.grpcwithspring.domain.model.User;

public interface CreateUserUseCase {

    User createUser(CreateUserCommand command);

    record CreateUserCommand(String email, BigDecimal initialBalance) {

        public CreateUserCommand {
            Objects.requireNonNull(email, "email must not be null");
            Objects.requireNonNull(initialBalance, "initialBalance must not be null");
        }

    }

}
