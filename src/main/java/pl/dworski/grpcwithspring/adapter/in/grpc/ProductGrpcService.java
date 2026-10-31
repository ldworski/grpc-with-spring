package pl.dworski.grpcwithspring.adapter.in.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import pl.dworski.grpcwithspring.adapter.in.grpc.mapper.ProductGrpcMapper;
import pl.dworski.grpcwithspring.domain.port.in.CreateProductUseCase;
import pl.dworski.grpcwithspring.domain.port.in.GetProductsUseCase;
import pl.dworski.grpcwithspring.proto.*;

@Service
class ProductGrpcService extends ProductServiceGrpc.ProductServiceImplBase {

    private final CreateProductUseCase createProductUseCase;
    private final GetProductsUseCase getProductsUseCase;

    ProductGrpcService(CreateProductUseCase createProductUseCase, GetProductsUseCase getProductsUseCase) {
        this.createProductUseCase = createProductUseCase;
        this.getProductsUseCase = getProductsUseCase;
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

    @Override
    public void getProducts(GetProductsRequest request, StreamObserver<GetProductsResponse> responseObserver) {
        try {
            var page = getProductsUseCase.getProducts(ProductGrpcMapper.toQuery(request));
            responseObserver.onNext(ProductGrpcMapper.toResponse(page));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

}
