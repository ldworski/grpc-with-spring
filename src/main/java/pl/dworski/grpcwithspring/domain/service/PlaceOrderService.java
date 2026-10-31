package pl.dworski.grpcwithspring.domain.service;

import pl.dworski.grpcwithspring.domain.exception.InsufficientStockException;
import pl.dworski.grpcwithspring.domain.exception.ProductNotFoundException;
import pl.dworski.grpcwithspring.domain.exception.UserNotFoundException;
import pl.dworski.grpcwithspring.domain.model.Order;
import pl.dworski.grpcwithspring.domain.model.OrderItem;
import pl.dworski.grpcwithspring.domain.port.in.PlaceOrderUseCase;
import pl.dworski.grpcwithspring.domain.port.out.OrderRepository;
import pl.dworski.grpcwithspring.domain.port.out.ProductRepository;
import pl.dworski.grpcwithspring.domain.port.out.TransactionRunner;
import pl.dworski.grpcwithspring.domain.port.out.UserRepository;

public class PlaceOrderService implements PlaceOrderUseCase {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final TransactionRunner transactionRunner;

    public PlaceOrderService(UserRepository userRepository, ProductRepository productRepository,
                             OrderRepository orderRepository, TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Order placeOrder(PlaceOrderCommand command) {
        return transactionRunner.inTransaction(() -> {
            if (userRepository.findById(command.userId()).isEmpty()) {
                throw new UserNotFoundException(command.userId());
            }
            command.items().forEach(this::reserveStock);
            return orderRepository.save(Order.create(command.userId(), command.items()));
        });
    }

    private void reserveStock(OrderItem item) {
        if (productRepository.decreaseStockIfAvailable(item.productId(), item.quantity())) {
            return;
        }
        var product = productRepository.findById(item.productId())
                .orElseThrow(() -> new ProductNotFoundException(item.productId()));
        throw new InsufficientStockException(item.productId(), item.quantity(), product.stockQuantity());
    }

}
