package pl.dworski.grpcwithspring.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record User(String id, String email, BigDecimal balance) {

    public User {
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(balance, "balance must not be null");
        if (email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (balance.signum() < 0) {
            throw new IllegalArgumentException("balance must not be negative");
        }
    }

    public static User create(String email, BigDecimal balance) {
        return new User(null, email, balance);
    }

}
