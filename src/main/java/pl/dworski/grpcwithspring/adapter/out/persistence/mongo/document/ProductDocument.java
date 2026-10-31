package pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import pl.dworski.grpcwithspring.domain.model.Product;

@Document("products")
public record ProductDocument(@Id String id, String name, int stockQuantity) {

    public static ProductDocument fromDomain(Product product) {
        return new ProductDocument(product.id(), product.name(), product.stockQuantity());
    }

    public Product toDomain() {
        return new Product(id, name, stockQuantity);
    }

}
