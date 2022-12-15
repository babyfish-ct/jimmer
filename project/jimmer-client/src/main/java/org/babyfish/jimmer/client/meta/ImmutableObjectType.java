package org.babyfish.jimmer.client.meta;

import org.babyfish.jimmer.meta.ImmutableType;

public interface ImmutableObjectType extends ObjectType {

    ImmutableType getImmutableType();

    Category getCategory();

    FetchByInfo getFetchByInfo();

    @Override
    default boolean hasDefinition() {
        return getCategory() == Category.RAW;
    }

    enum Category {
        FETCH,
        VIEW,
        RAW
    }
}
