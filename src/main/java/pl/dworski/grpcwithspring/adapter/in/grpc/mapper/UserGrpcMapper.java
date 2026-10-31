package pl.dworski.grpcwithspring.adapter.in.grpc.mapper;

import pl.dworski.grpcwithspring.domain.model.User;
import pl.dworski.grpcwithspring.domain.port.in.CreateUserUseCase.CreateUserCommand;
import pl.dworski.grpcwithspring.proto.CreateUserRequest;
import pl.dworski.grpcwithspring.proto.CreateUserResponse;

import java.math.BigDecimal;

public final class UserGrpcMapper {

    private UserGrpcMapper() {
    }

    public static CreateUserCommand toCommand(CreateUserRequest request) {
        return new CreateUserCommand(request.getEmail(), new BigDecimal(request.getInitialBalance()));
    }

    public static CreateUserResponse toResponse(User user) {
        return CreateUserResponse.newBuilder()
                .setUser(pl.dworski.grpcwithspring.proto.User.newBuilder()
                        .setId(user.id())
                        .setEmail(user.email())
                        .setBalance(user.balance().toPlainString()))
                .build();
    }

}
