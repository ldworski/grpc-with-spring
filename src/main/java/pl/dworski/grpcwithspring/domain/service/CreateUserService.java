package pl.dworski.grpcwithspring.domain.service;

import pl.dworski.grpcwithspring.domain.exception.UserAlreadyExistsException;
import pl.dworski.grpcwithspring.domain.model.User;
import pl.dworski.grpcwithspring.domain.port.in.CreateUserUseCase;
import pl.dworski.grpcwithspring.domain.port.out.UserRepository;

public class CreateUserService implements CreateUserUseCase {

    private final UserRepository userRepository;

    public CreateUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(CreateUserCommand command) {
        if (userRepository.findByEmail(command.email()).isPresent()) {
            throw new UserAlreadyExistsException(command.email());
        }
        return userRepository.save(User.create(command.email(), command.initialBalance()));
    }

}
