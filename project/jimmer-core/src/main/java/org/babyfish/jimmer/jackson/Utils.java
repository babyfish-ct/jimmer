package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.meata.ImmutableProp;

import java.util.List;

class Utils {

    public static JavaType getJacksonType(ImmutableProp prop) {
        if (prop.isEntityList() || prop.isScalarList()) {
            return CollectionType.construct(
                    List.class,
                    null,
                    null,
                    null,
                    SimpleType.constructUnsafe(prop.getElementClass())
            );
        }
        return SimpleType.constructUnsafe(prop.getElementClass());
    }
}
