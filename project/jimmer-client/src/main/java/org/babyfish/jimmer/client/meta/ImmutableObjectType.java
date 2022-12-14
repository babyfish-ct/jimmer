package org.babyfish.jimmer.client.meta;

import org.babyfish.jimmer.meta.ImmutableType;

public interface ImmutableObjectType extends ObjectType {

    boolean isAnonymous();

    ImmutableType getImmutableType();

    Category getCategory();

    @Override
    default boolean hasDefinition() {
        return !isAnonymous();
    }

    enum Category {
        FETCH,
        VIEW,
        RAW
    }
}
