package pl.dworski.grpcwithspring.adapter.in.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import pl.dworski.grpcwithspring.adapter.in.grpc.mapper.OrderGrpcMapper;
import pl.dworski.grpcwithspring.domain.exception.InsufficientStockException;
import pl.dworski.grpcwithspring.domain.exception.ProductNotFoundException;
import pl.dworski.grpcwithspring.domain.exception.TransactionConflictException;
import pl.dworski.grpcwithspring.domain.exception.UserNotFoundException;
import pl.dworski.grpcwithspring.domain.port.in.PlaceOrderUseCase;
import pl.dworski.grpcwithspring.proto.OrderServiceGrpc;
import pl.dworski.grpcwithspring.proto.PlaceOrderRequest;
import pl.dworski.grpcwithspring.proto.PlaceOrderResponse;

@Service
class OrderGrpcService extends OrderServiceGrpc.OrderServiceImplBase {

    private final PlaceOrderUseCase placeOrderUseCase;

    OrderGrpcService(PlaceOrderUseCase placeOrderUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
    }

    @Override
    public void placeOrder(PlaceOrderRequest request, StreamObserver<PlaceOrderResponse> responseObserver) {
        try {
            var order = placeOrderUseCase.placeOrder(OrderGrpcMapper.toCommand(request));
            responseObserver.onNext(OrderGrpcMapper.toResponse(order));
            responseObserver.onCompleted();
        } catch (UserNotFoundException | ProductNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (InsufficientStockException e) {
            responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
        } catch (TransactionConflictException e) {
            responseObserver.onError(Status.ABORTED.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

}
