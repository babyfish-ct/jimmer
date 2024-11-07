package org.babyfish.jimmer.jackson;

import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

import java.lang.reflect.Method;

class ImmutableProps {

    private ImmutableProps() {}

    static ImmutableProp get(ImmutableType type, Method method) {
        String propName = StringUtil.propName(method.getName(), false);
        if (propName == null) {
            propName = method.getName();
        }
        ImmutableProp prop = type.getProps().get(propName);
        if (prop == null && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
            propName = StringUtil.propName(method.getName(), true);
            prop = type.getProps().get(propName);
        }
        if (prop == null && propName != null) {
            boolean conflict = false;
            for (ImmutableProp p : type.getProps().values()) {
                if (p.getName().equalsIgnoreCase(propName)) {
                    if (prop == null) {
                        prop = p;
                    } else {
                        conflict = true;
                    }
                }
            }
            if (conflict) {
                prop = null;
            }
        }
        if (prop == null) {
            throw new IllegalArgumentException("There is no jimmer property for " + method);
        }
        return prop;
    }
}
