package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.File;
import org.babyfish.jimmer.client.meta.ImmutableObjectType;
import org.babyfish.jimmer.client.meta.NullableType;
import org.babyfish.jimmer.client.meta.Property;

import java.util.List;

public class DtoWriter extends TsCodeWriter {

    private final List<ImmutableObjectType> immutableObjectTypes;

    private final boolean mutable;

    public DtoWriter(TsContext ctx, Class<?> rawType, boolean mutable) {
        super(ctx, dtoFile(ctx, rawType), mutable);
        this.mutable = mutable;
        this.immutableObjectTypes = ctx.getDtoMap().get(rawType);
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
                        codeIf(!mutable, "readonly ")
                                .code(prop.getName())
                                .codeIf(prop.getType() instanceof NullableType, '?')
                                .code(": ")
                                .typeRef(NullableType.unwrap(prop.getType()));
                    }
                });
            }
        });
    }
}
