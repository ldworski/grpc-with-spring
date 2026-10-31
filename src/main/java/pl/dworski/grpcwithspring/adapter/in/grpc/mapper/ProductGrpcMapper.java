package pl.dworski.grpcwithspring.adapter.in.grpc.mapper;

import pl.dworski.grpcwithspring.domain.model.Product;
import pl.dworski.grpcwithspring.domain.model.paging.Page;
import pl.dworski.grpcwithspring.domain.model.paging.PageQuery;
import pl.dworski.grpcwithspring.domain.port.in.CreateProductUseCase.CreateProductCommand;
import pl.dworski.grpcwithspring.domain.port.in.GetProductsUseCase.GetProductsQuery;
import pl.dworski.grpcwithspring.proto.CreateProductRequest;
import pl.dworski.grpcwithspring.proto.CreateProductResponse;
import pl.dworski.grpcwithspring.proto.GetProductsRequest;
import pl.dworski.grpcwithspring.proto.GetProductsResponse;

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
                .setProduct(toProto(product))
                .build();
    }

    public static GetProductsQuery toQuery(GetProductsRequest request) {
        return new GetProductsQuery(PageQuery.of(
                request.hasPage() ? request.getPage() : null,
                request.hasPageSize() ? request.getPageSize() : null));
    }

    public static GetProductsResponse toResponse(Page<Product> page) {
        return GetProductsResponse.newBuilder()
                .addAllProducts(page.content().stream().map(ProductGrpcMapper::toProto).toList())
                .setPage(page.page())
                .setPageSize(page.size())
                .setTotalElements(page.totalElements())
                .setTotalPages(page.totalPages())
                .build();
    }

    private static pl.dworski.grpcwithspring.proto.Product toProto(Product product) {
        return pl.dworski.grpcwithspring.proto.Product.newBuilder()
                .setId(product.id())
                .setName(product.name())
                .setStockQuantity(product.stockQuantity())
                .build();
    }

}
