package org.babyfish.jimmer.client.meta;

import org.babyfish.jimmer.meta.ImmutableType;

public interface ImmutableObjectType extends ObjectType {

    @Override
    Class<?> getJavaType();

    ImmutableType getImmutableType();

    Category getCategory();

    enum Category {
        FETCH,
        VIEW,
        RAW
    }
}
