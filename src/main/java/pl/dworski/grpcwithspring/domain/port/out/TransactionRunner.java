package pl.dworski.grpcwithspring.domain.port.out;

import java.util.function.Supplier;

public interface TransactionRunner {

    <T> T inTransaction(Supplier<T> action);

}
