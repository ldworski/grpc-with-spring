package pl.dworski.grpcwithspring.domain.port.in;

import pl.dworski.grpcwithspring.domain.model.Product;
import pl.dworski.grpcwithspring.domain.model.paging.Page;
import pl.dworski.grpcwithspring.domain.model.paging.PageQuery;

import java.util.Objects;

public interface GetProductsUseCase {

    Page<Product> getProducts(GetProductsQuery query);

    record GetProductsQuery(PageQuery pageQuery) {

        public GetProductsQuery {
            Objects.requireNonNull(pageQuery, "pageQuery must not be null");
        }

    }

}
