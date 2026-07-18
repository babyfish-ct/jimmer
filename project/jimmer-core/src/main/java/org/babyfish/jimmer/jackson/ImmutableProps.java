package org.babyfish.jimmer.jackson;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

import java.lang.reflect.Method;

/**
 * @deprecated 请使用 {@link org.babyfish.jimmer.json.ImmutableProps}。
 */
@Deprecated
public class ImmutableProps {

    private ImmutableProps() {
    }

    public static ImmutableProp get(ImmutableType type, Method method) {
        return org.babyfish.jimmer.json.ImmutableProps.get(type, method);
    }
}
