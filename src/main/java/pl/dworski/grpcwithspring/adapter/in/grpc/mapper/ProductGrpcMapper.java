package pl.dworski.grpcwithspring.adapter.in.grpc.mapper;

import pl.dworski.grpcwithspring.domain.model.Product;
import pl.dworski.grpcwithspring.domain.port.in.CreateProductUseCase.CreateProductCommand;
import pl.dworski.grpcwithspring.proto.CreateProductRequest;
import pl.dworski.grpcwithspring.proto.CreateProductResponse;

public final class ProductGrpcMapper {

    private ProductGrpcMapper() {
    }

    public static CreateProductCommand toCommand(CreateProductRequest request) {
        if (!request.hasInitialStockQuantity()) {
            throw new IllegalArgumentException("initial_stock_quantity is required");
        }
        return new CreateProductCommand(request.getName(), request.getInitialStockQuantity());
    }

    public static CreateProductResponse toResponse(Product product) {
        return CreateProductResponse.newBuilder()
                .setProduct(pl.dworski.grpcwithspring.proto.Product.newBuilder()
                        .setId(product.id())
                        .setName(product.name())
                        .setStockQuantity(product.stockQuantity()))
                .build();
    }

}
