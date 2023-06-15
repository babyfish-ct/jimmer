package org.babyfish.jimmer.spring.cache.impl;

import org.babyfish.jimmer.sql.cache.TransactionCacheOperator;
import org.babyfish.jimmer.sql.event.DatabaseEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

public class TransactionCacheOperatorFlusher {

    private final List<TransactionCacheOperator> operators;

    private final ThreadLocal<Boolean> dirtyLocal = new ThreadLocal<>();

    public TransactionCacheOperatorFlusher(List<TransactionCacheOperator> operators) {
        if (operators.isEmpty()) {
            throw new IllegalArgumentException("`operators` cannot be empty");
        }
        this.operators = operators;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void beforeCommit(DatabaseEvent e) {
        dirtyLocal.set(Boolean.TRUE);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(DatabaseEvent e) {
        if (dirtyLocal.get() != null) {
            dirtyLocal.remove();
            flush();
        }
    }

    @Scheduled(
            fixedDelayString = "${jimmer.transaction-cache-operator-fixed-delay:5000}",
            initialDelay = 0
    )
    public void retry() {
        flush();
    }

    private void flush() {
        if (operators.size() == 1) {
            TransactionCacheOperator operator = operators.get(0);
            operator.flush();
        } else {
            Throwable throwable = null;
            for (TransactionCacheOperator operator : operators) {
                try {
                    operator.flush();
                } catch (RuntimeException | Error ex) {
                    if (throwable == null) {
                        throwable = ex;
                    }
                }
            }
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            }
            if (throwable != null) {
                throw (Error)throwable;
            }
        }
    }
}
