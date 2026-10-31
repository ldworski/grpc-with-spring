package pl.dworski.grpcwithspring.adapter.in.grpc.mapper;

import pl.dworski.grpcwithspring.domain.model.Order;
import pl.dworski.grpcwithspring.domain.model.OrderItem;
import pl.dworski.grpcwithspring.domain.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import pl.dworski.grpcwithspring.proto.PlaceOrderRequest;
import pl.dworski.grpcwithspring.proto.PlaceOrderResponse;

public final class OrderGrpcMapper {

    private OrderGrpcMapper() {
    }

    public static PlaceOrderCommand toCommand(PlaceOrderRequest request) {
        return new PlaceOrderCommand(request.getUserId(), request.getItemsList().stream()
                .map(item -> new OrderItem(item.getProductId(), item.getQuantity()))
                .toList());
    }

    public static PlaceOrderResponse toResponse(Order order) {
        return PlaceOrderResponse.newBuilder()
                .setOrder(pl.dworski.grpcwithspring.proto.Order.newBuilder()
                        .setId(order.id())
                        .setUserId(order.userId())
                        .addAllItems(order.items().stream().map(OrderGrpcMapper::toProto).toList()))
                .build();
    }

    private static pl.dworski.grpcwithspring.proto.OrderItem toProto(OrderItem item) {
        return pl.dworski.grpcwithspring.proto.OrderItem.newBuilder()
                .setProductId(item.productId())
                .setQuantity(item.quantity())
                .build();
    }

}
