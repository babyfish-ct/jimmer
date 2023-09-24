package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.Draft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simpler {@link DraftHandler}, it will not query original data,
 * only tell you whether the current operation is insert.
 * @param <D>
 */
public interface DraftInterceptor<D extends Draft> {

    /**
     * Adjust draft before save
     * @param draft The draft can be modified, `id` and `key` properties cannot be changed, otherwise, exception will be raised.
     * @param isNew Whether the current operation is insert.
     */
    void beforeSave(@NotNull D draft, boolean isNew);

    /**
     * Should not be used by user
     */
    static <D extends Draft> DraftHandler<D, ?> wrap(DraftInterceptor<D> interceptor) {
        if (interceptor == null) {
            return null;
        }
        return new DraftInterceptorWrapper<>(interceptor);
    }

    /**
     * Should not be used by user
     */
    @SuppressWarnings("unchecked")
    static <D extends Draft> DraftInterceptor<D> unwrap(DraftHandler<D, ?> handler) {
        if (!(handler instanceof DraftInterceptorWrapper<?>)) {
            return null;
        }
        return ((DraftInterceptorWrapper<D>) handler).interceptor;
    }
}

final class DraftInterceptorWrapper<D extends Draft> implements DraftHandler<D, Object> {

    final DraftInterceptor<D> interceptor;

    DraftInterceptorWrapper(DraftInterceptor<D> interceptor) {
        if (interceptor instanceof DraftHandler<?, ?>) {
            throw new IllegalStateException(
                    "The type \"" +
                            interceptor.getClass().getName() +
                            "\" cannot implement both \"" +
                            DraftInterceptor.class.getName() +
                            "\" and \"" +
                            DraftHandler.class.getName() +
                            "\""
            );
        }

        this.interceptor = interceptor;
    }

    @Override
    public void beforeSave(@NotNull D draft, @Nullable Object original) {
        interceptor.beforeSave(draft, original == null);
    }

    @Override
    public int hashCode() {
        return interceptor.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DraftInterceptorWrapper<?> that = (DraftInterceptorWrapper<?>) o;
        return interceptor.equals(that.interceptor);
    }

    @Override
    public String toString() {
        return "DraftInterceptorWrapper{" +
                "interceptor=" + interceptor +
                '}';
    }
}