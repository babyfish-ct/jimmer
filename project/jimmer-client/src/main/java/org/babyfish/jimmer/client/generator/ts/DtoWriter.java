package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.meta.ImmutableObjectType;
import org.babyfish.jimmer.client.meta.NullableType;
import org.babyfish.jimmer.client.meta.Property;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

import java.util.List;

public class DtoWriter extends CodeWriter {

    private final List<ImmutableObjectType> immutableObjectTypes;

    public DtoWriter(Context ctx, Class<?> rawType, List<ImmutableObjectType> immutableObjectTypes) {
        super(ctx, dtoFile(ctx, rawType));
        this.immutableObjectTypes = immutableObjectTypes;
    }

    public static File dtoFile(Context ctx, Class<?> rawType) {
        return new File("model/dto", ctx.getDtoPrefix(rawType));
    }

    @Override
    protected void write() {
        code("export type ").code(getFile().getName()).code(" = ");
        scope(ScopeType.OBJECT, ", ", true, () -> {
            for (ImmutableObjectType type : immutableObjectTypes) {
                separator();
                code('\'').code(getContext().getDtoSuffix(type)).code("': ");
                scope(ScopeType.OBJECT, ", ", true, () -> {
                    for (Property prop : type.getProperties().values()) {
                        separator();
                        code("readonly ")
                                .code(prop.getName())
                                .codeIf(prop.getType() instanceof NullableType, '?')
                                .code(": ")
                                .type(NullableType.unwrap(prop.getType()));
                    }
                });
            }
        });
    }
}
