package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractExecutorProxy implements Executor {

    protected final Executor raw;

    protected AbstractExecutorProxy(Executor raw) {
        this.raw = raw;
    }

    @Override
    public final BatchContext executeBatch(
            @NotNull Connection con,
            @NotNull String sql,
            @Nullable ImmutableProp generatedIdProp,
            @NotNull ExecutionPurpose purpose,
            @NotNull JSqlClientImplementor sqlClient
    ) {
        return createBatch(
                raw.executeBatch(
                        con,
                        sql,
                        generatedIdProp,
                        purpose,
                        sqlClient
                )
        );
    }

    protected abstract AbstractExecutorProxy recreate(Executor raw);

    protected abstract Batch createBatch(BatchContext raw);

    protected static abstract class Batch implements BatchContext {

        protected final BatchContext raw;

        protected Batch(BatchContext raw) {
            this.raw = raw;
        }

        @Override
        public JSqlClientImplementor sqlClient() {
            return raw.sqlClient();
        }

        @Override
        public String sql() {
            return raw.sql();
        }

        @Override
        public ExecutionPurpose purpose() {
            return raw.purpose();
        }

        @Override
        public ExecutorContext ctx() {
            return raw.ctx();
        }

        @Override
        public void add(List<Object> variables) {
            raw.add(variables);
        }

        @Override
        public int[] execute(BiFunction<SQLException, BatchContext, Exception> exceptionTranslator) {
            return raw.execute(exceptionTranslator);
        }

        @Override
        public Object[] generatedIds() {
            return raw.generatedIds();
        }

        @Override
        public void addExecutedListener(Runnable listener) {
            raw.addExecutedListener(listener);
        }

        @Override
        public void close() {
            raw.close();
        }
    }

    public interface Applier {
        Executor applyTo(Executor executor);
    }

    protected static <P extends AbstractExecutorProxy> Applier applier(
            Class<P> proxyType,
            Predicate<P> reuse,
            Function<Executor, P> creator
    ) {
        return new ApplierImpl<>(proxyType, reuse, creator);
    }

    @SuppressWarnings("unchecked")
    public static <E extends Executor> E as(Executor executor, Class<E> proxyType) {
        if (proxyType.isAssignableFrom(executor.getClass())) {
            return (E) executor;
        }
        if (executor instanceof AbstractExecutorProxy) {
            return as(((AbstractExecutorProxy)executor).raw, proxyType);
        }
        return null;
    }

    private static class ApplierImpl<P extends AbstractExecutorProxy> implements Applier {

        private final Class<P> proxyType;

        private final Predicate<P> reuse;

        private final Function<Executor, P> creator;

        private ApplierImpl(Class<P> proxyType, Predicate<P> reuse, Function<Executor, P> creator) {
            if (Modifier.isAbstract(proxyType.getModifiers())) {
                throw new IllegalArgumentException(
                        "Cannot create proxy applier for proxy type \"" +
                                proxyType.getName() +
                                "\", it should not be abstract type"
                );
            }
            this.proxyType = proxyType;
            this.reuse = reuse;
            this.creator = creator;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Executor applyTo(Executor executor) {
            if (executor == null) {
                executor = DefaultExecutor.INSTANCE;
            }

            List<Executor> executors = new ArrayList<>();
            expand(executor, executors);

            ListIterator<Executor> itr = executors.listIterator(executors.size());
            Executor prev = null;
            boolean recreated = false;
            while (itr.hasPrevious()) {
                Executor cur = itr.previous();
                if (recreated) {
                    cur = ((AbstractExecutorProxy)cur).recreate(prev);
                } else if (proxyType.isAssignableFrom(cur.getClass())) {
                    if (reuse.test((P)cur)) {
                        return executor;
                    }
                    cur = creator.apply(prev);
                    recreated = true;
                }
                prev = cur;
            }
            if (recreated) {
                return prev;
            }
            return creator.apply(executor);
        }

        private static void expand(Executor executor, List<Executor> outputExecutors) {
            outputExecutors.add(executor);
            if (executor instanceof AbstractExecutorProxy) {
                AbstractExecutorProxy proxy = (AbstractExecutorProxy) executor;
                expand(proxy.raw, outputExecutors);
            }
        }
    }
}
