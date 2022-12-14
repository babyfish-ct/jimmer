package org.babyfish.jimmer.client.meta;

import java.util.List;

public interface EnumType extends Type {

    Class<?> getJavaType();

    List<String> getItems();

    @Override
    default boolean hasDefinition() {
        return true;
    }
}
