package pl.dworski.grpcwithspring.domain.model.paging;

import org.jspecify.annotations.Nullable;

public record PageQuery(int page, int size) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
        size = Math.min(size, MAX_SIZE);
    }

    public static PageQuery of(@Nullable Integer page, @Nullable Integer size) {
        return new PageQuery(page != null ? page : DEFAULT_PAGE, size != null ? size : DEFAULT_SIZE);
    }

}
