package pl.dworski.grpcwithspring.adapter.in.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import pl.dworski.grpcwithspring.adapter.in.grpc.mapper.ProductGrpcMapper;
import pl.dworski.grpcwithspring.domain.port.in.CreateProductUseCase;
import pl.dworski.grpcwithspring.proto.CreateProductRequest;
import pl.dworski.grpcwithspring.proto.CreateProductResponse;
import pl.dworski.grpcwithspring.proto.ProductServiceGrpc;

@Service
class ProductGrpcService extends ProductServiceGrpc.ProductServiceImplBase {

    private final CreateProductUseCase createProductUseCase;

    ProductGrpcService(CreateProductUseCase createProductUseCase) {
        this.createProductUseCase = createProductUseCase;
    }

    @Override
    public void createProduct(CreateProductRequest request, StreamObserver<CreateProductResponse> responseObserver) {
        try {
            var product = createProductUseCase.createProduct(ProductGrpcMapper.toCommand(request));
            responseObserver.onNext(ProductGrpcMapper.toResponse(product));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

}
