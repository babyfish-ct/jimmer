package org.babyfish.jimmer.client.meta;

import java.util.Map;

public interface ObjectType extends Type {

    Class<?> getJavaType();

    boolean isEntity();

    Map<String, Property> getProperties();
}
