package org.babyfish.jimmer.spring.repository.support;

import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.core.EntityInformation;

public class JimmerEntityInformation<E, ID> implements EntityInformation<E, ID> {

    private final ImmutableType immutableType;

    public JimmerEntityInformation(ImmutableType immutableType) {
        this.immutableType = immutableType;
    }

    @Override
    public boolean isNew(@NotNull E entity) {
        throw new UnsupportedOperationException(
                "`" +
                        JimmerEntityInformation.class.getName() +
                        ".isNew` is not supported, " +
                        "jimmer has a special mechanism for " +
                        "judging whether to insert or update"
        );
    }

    @Override
    public ID getId(@NotNull E entity) {
        return null;
    }

    @NotNull
    @Override
    public Class<ID> getIdType() {
        return null;
    }

    @NotNull
    @Override
    public Class<E> getJavaType() {
        return null;
    }
}
