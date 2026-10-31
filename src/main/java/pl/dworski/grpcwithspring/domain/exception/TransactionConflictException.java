package pl.dworski.grpcwithspring.domain.exception;

public class TransactionConflictException extends RuntimeException {

    public TransactionConflictException(Throwable cause) {
        super("Transaction was aborted by concurrent modifications, please try again", cause);
    }

}
