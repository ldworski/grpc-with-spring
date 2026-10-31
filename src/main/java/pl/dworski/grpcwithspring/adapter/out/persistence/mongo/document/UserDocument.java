package pl.dworski.grpcwithspring.adapter.out.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import pl.dworski.grpcwithspring.domain.model.User;

import java.math.BigDecimal;

@Document("users")
public record UserDocument(
        @Id String id,
        @Indexed(unique = true) String email,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal balance) {

    public static UserDocument fromDomain(User user) {
        return new UserDocument(user.id(), user.email(), user.balance());
    }

    public User toDomain() {
        return new User(id, email, balance);
    }

}
