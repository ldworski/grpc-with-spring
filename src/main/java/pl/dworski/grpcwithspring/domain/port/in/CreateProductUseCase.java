package pl.dworski.grpcwithspring.domain.port.in;

import java.util.Objects;
import pl.dworski.grpcwithspring.domain.model.Product;

public interface CreateProductUseCase {

    Product createProduct(CreateProductCommand command);

    record CreateProductCommand(String name, int initialStockQuantity) {

        public CreateProductCommand {
            Objects.requireNonNull(name, "name must not be null");
        }

    }

}
