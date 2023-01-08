package org.babyfish.jimmer.client.generator.java.feign;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.java.JavaCodeWriter;
import org.babyfish.jimmer.client.generator.ts.File;
import org.babyfish.jimmer.client.meta.*;

import java.util.List;

public class DtoWriter extends JavaCodeWriter<FeignContext> {

    private final List<ImmutableObjectType> immutableObjectTypes;

    public DtoWriter(FeignContext ctx, Class<?> rawType) {
        super(ctx, dtoFile(ctx, rawType));
        this.immutableObjectTypes = ctx.getDtoMap().get(rawType);
    }

    public static File dtoFile(Context ctx, Class<?> rawType) {
        return new File("model/dto", ctx.getDtoPrefix(rawType));
    }

    @Override
    protected void write() {

        code("\npublic interface ").code(file.getName()).code(' ');
        scope(ScopeType.OBJECT, "", true, () -> {
            for (ImmutableObjectType immutableObjectType : immutableObjectTypes) {
                write(immutableObjectType);
            }
        });
    }

    private void write(ImmutableObjectType type) {
        code("\nclass ")
                .code(getContext().getDtoSuffix(type).replace('/', '_'))
                .code(' ');
        scope(ScopeType.OBJECT, "", true, () -> {
            writeProperties(type);
            writeTargetTypeDeclarations(type);
        });
        code('\n');
    }

    private void writeProperties(ImmutableObjectType type) {
        for (Property prop : type.getProperties().values()) {
            handleDtoProp(prop, () -> {
                writeProperty(prop);
            });
        }
    }

    private void writeProperty(Property prop) {
        if (prop.getType() instanceof NullableType) {
            code("\n@Nullable");
        }
        code("\nprivate ");
        typeRef(prop.getType());
        code(' ').code(prop.getName()).code(";\n");
    }

    private void writeTargetTypeDeclarations(ImmutableObjectType type) {
        for (Property prop : type.getProperties().values()) {
            handleDtoProp(prop, () -> {
                writeTargetTypeDeclaration(prop.getType());
            });
        }
    }

    private void writeTargetTypeDeclaration(Type type) {
        if (type instanceof NullableType) {
            writeTargetTypeDeclaration(((NullableType)type).getTargetType());
        } else if (type instanceof ArrayType) {
            writeTargetTypeDeclaration(((ArrayType)type).getElementType());
        } else if (type instanceof MapType) {
            writeTargetTypeDeclaration(((MapType)type).getKeyType());
            writeTargetTypeDeclaration(((MapType)type).getValueType());
        } else if (type instanceof ImmutableObjectType) {
            code("\npublic static class ").code(tempDtoTypeName()).code(' ');
            scope(ScopeType.OBJECT, "", true, () -> {
                writeProperties((ImmutableObjectType) type);
                writeTargetTypeDeclarations((ImmutableObjectType) type);
            });
            code('\n');
        }
    }
}
