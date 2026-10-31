package pl.dworski.grpcwithspring.domain.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String userId) {
        super("User with id '%s' does not exist".formatted(userId));
    }

}
