package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ChangedRef<T> {

    private final T oldValue;

    private final T newValue;

    public ChangedRef(T oldValue, T newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Nullable
    public T getOldValue() {
        return oldValue;
    }

    @Nullable
    public T getNewValue() {
        return newValue;
    }

    @SuppressWarnings("unchecked")
    public <X> ChangedRef<X> toIdRef() {
        ImmutableType type;
        if (oldValue instanceof ImmutableSpi) {
            type = ((ImmutableSpi) oldValue).__type();
        } else if (newValue instanceof ImmutableSpi) {
            type = ((ImmutableSpi) newValue).__type();
        } else {
            throw new IllegalStateException("The current `ChangedRef` is not object pair");
        }
        int idPropId = type.getIdProp().getId();
        return new ChangedRef<>(
                oldValue != null ? (X)((ImmutableSpi) oldValue).__get(idPropId) : null,
                newValue != null ? (X)((ImmutableSpi) newValue).__get(idPropId) : null
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangedRef<?> that = (ChangedRef<?>) o;
        return Objects.equals(oldValue, that.oldValue) && Objects.equals(newValue, that.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldValue, newValue);
    }

    @Override
    public String toString() {
        return "ChangedRef{" +
                "oldValue=" + oldValue +
                ", newValue=" + newValue +
                '}';
    }
}
