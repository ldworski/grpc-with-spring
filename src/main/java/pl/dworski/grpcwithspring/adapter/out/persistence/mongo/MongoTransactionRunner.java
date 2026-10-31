package pl.dworski.grpcwithspring.adapter.out.persistence.mongo;

import com.mongodb.MongoException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import pl.dworski.grpcwithspring.domain.exception.TransactionConflictException;
import pl.dworski.grpcwithspring.domain.port.out.TransactionRunner;

import java.time.Duration;
import java.util.function.Supplier;

@Component
class MongoTransactionRunner implements TransactionRunner {

    private static final RetryPolicy TRANSIENT_ERROR_RETRY_POLICY = RetryPolicy.builder()
            .maxRetries(4)
            .delay(Duration.ofMillis(20))
            .multiplier(2)
            .jitter(Duration.ofMillis(10))
            .predicate(MongoTransactionRunner::isTransientTransactionError)
            .build();

    private final TransactionTemplate transactionTemplate;
    private final RetryTemplate retryTemplate = new RetryTemplate(TRANSIENT_ERROR_RETRY_POLICY);

    MongoTransactionRunner(MongoTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T inTransaction(Supplier<T> action) {
        try {
            return retryTemplate.invoke(() -> transactionTemplate.execute(_ -> action.get()));
        } catch (RuntimeException e) {
            if (isTransientTransactionError(e)) {
                throw new TransactionConflictException(e);
            }
            throw e;
        }
    }

    private static boolean isTransientTransactionError(Throwable throwable) {
        for (var cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof MongoException mongoException
                    && mongoException.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)) {
                return true;
            }
        }
        return false;
    }

}
