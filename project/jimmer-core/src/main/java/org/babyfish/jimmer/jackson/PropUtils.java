package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;

import java.util.List;

public class PropUtils {

    public static JavaType getJacksonType(ImmutableProp prop) {
        if (prop.isReferenceList(TargetLevel.OBJECT) || prop.isScalarList()) {
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
