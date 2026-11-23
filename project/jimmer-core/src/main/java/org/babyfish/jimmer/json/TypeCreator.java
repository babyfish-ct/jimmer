package org.babyfish.jimmer.json;

import java.lang.reflect.Type;

@FunctionalInterface
public interface TypeCreator {

    Type createType(JsonTypeFactory factory);
}
