package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.MethodSpec;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;

public class CaseAppender {

    private final MethodSpec.Builder builder;

    private final ImmutableType type;

    private final Class<?> argType;

    public CaseAppender(MethodSpec.Builder builder, ImmutableType type, Class<?> argType) {
        this.builder = builder;
        this.type = type;
        this.argType = argType;
    }

    public void addCase(ImmutableProp prop) {
        if (argType == int.class) {
            ImmutableType declaringType = prop.getDeclaringType();
            if (declaringType == type) {
                builder.addCode("case $L:\n\t\t", prop.getId());
            } else {
                builder.addCode("case $T.SLOT_$L:\n\t\t", declaringType.getProducerClassName(), Strings.upper(prop.getName()));
            }
        } else {
            builder.addCode("case $S:\n\t\t", prop.getName());
        }
    }
}
