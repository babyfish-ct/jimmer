package org.babyfish.jimmer.sql.runtime;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExecutorProxyTest {

    @Test
    public void test() {
        Executor executor = DefaultExecutor.INSTANCE;

        executor = A.wrap(executor, "I");
        Assertions.assertEquals("A(I)->raw", executor.toString());
        executor = B.wrap(executor, "I");
        Assertions.assertEquals("B(I)->A(I)->raw", executor.toString());

        executor = A.wrap(executor, "II");
        Assertions.assertEquals("B(I)->A(II)->raw", executor.toString());
        executor = B.wrap(executor, "II");
        Assertions.assertEquals("B(II)->A(II)->raw", executor.toString());

        executor = A.wrap(executor, "III");
        Assertions.assertEquals("B(II)->A(III)->raw", executor.toString());
        executor = B.wrap(executor, "III");
        Assertions.assertEquals("B(III)->A(III)->raw", executor.toString());
    }

    private static class A extends AbstractExecutorProxy {

        private final String value;

        A(Executor raw, String value) {
            super(raw);
            this.value = value;
        }

        public static Executor wrap(Executor executor, String value) {
            return applier(
                    A.class,
                    p -> p.value.equals(value),
                    r -> new A(r, value)
            ).applyTo(executor);
        }

        @Override
        protected AbstractExecutorProxy recreate(Executor raw) {
            return new A(raw, value);
        }

        @Override
        protected Batch createBatch(BatchContext raw) {
            return new Batch(raw);
        }

        @Override
        public <R> R execute(@NotNull Args<R> args) {
            return null;
        }

        protected static class Batch extends AbstractExecutorProxy.Batch {

            protected Batch(BatchContext raw) {
                super(raw);
            }
        }

        @Override
        public String toString() {
            return "A(" + value + ")->" +
                    (raw instanceof AbstractExecutorProxy ? raw : "raw");
        }
    }

    private static class B extends AbstractExecutorProxy {

        private final String value;

        B(Executor raw, String value) {
            super(raw);
            this.value = value;
        }

        public static Executor wrap(Executor executor, String value) {
            return applier(
                    B.class,
                    p -> p.value.equals(value),
                    r -> new B(r, value)
            ).applyTo(executor);
        }

        @Override
        protected AbstractExecutorProxy recreate(Executor raw) {
            return new B(raw, value);
        }

        @Override
        protected Batch createBatch(BatchContext raw) {
            return new Batch(raw);
        }

        @Override
        public <R> R execute(@NotNull Args<R> args) {
            return null;
        }

        protected static class Batch extends AbstractExecutorProxy.Batch {

            protected Batch(BatchContext raw) {
                super(raw);
            }
        }

        @Override
        public String toString() {
            return "B(" + value + ")->" +
                    (raw instanceof AbstractExecutorProxy ? raw : "raw");
        }
    }
}
