package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface ObjectType extends Type {

    Class<?> getJavaType();

    boolean isEntity();

    Map<String, Property> getProperties();

    @Nullable
    Document getDocument();
}
