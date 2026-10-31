package pl.dworski.grpcwithspring.adapter.in.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import pl.dworski.grpcwithspring.adapter.in.grpc.mapper.UserGrpcMapper;
import pl.dworski.grpcwithspring.domain.exception.UserAlreadyExistsException;
import pl.dworski.grpcwithspring.domain.port.in.CreateUserUseCase;
import pl.dworski.grpcwithspring.proto.CreateUserRequest;
import pl.dworski.grpcwithspring.proto.CreateUserResponse;
import pl.dworski.grpcwithspring.proto.UserServiceGrpc;

@Service
class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final CreateUserUseCase createUserUseCase;

    UserGrpcService(CreateUserUseCase createUserUseCase) {
        this.createUserUseCase = createUserUseCase;
    }

    @Override
    public void createUser(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
        try {
            var user = createUserUseCase.createUser(UserGrpcMapper.toCommand(request));
            responseObserver.onNext(UserGrpcMapper.toResponse(user));
            responseObserver.onCompleted();
        } catch (UserAlreadyExistsException e) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

}
