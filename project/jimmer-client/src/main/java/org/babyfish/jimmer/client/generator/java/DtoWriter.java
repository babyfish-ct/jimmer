package org.babyfish.jimmer.client.generator.java;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.File;
import org.babyfish.jimmer.client.meta.*;

import java.util.List;

public class DtoWriter extends JavaCodeWriter<JavaContext> {

    private final List<ImmutableObjectType> immutableObjectTypes;

    public DtoWriter(JavaContext ctx, Class<?> rawType) {
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
            writeFields(type);
            writeProperties(type);
            writeTargetTypeDeclarations(type);
        });
        code('\n');
    }

    private void writeFields(ImmutableObjectType type) {
        for (Property prop : type.getProperties().values()) {
            handleDtoProp(prop, () -> {
                writeField(prop);
            });
        }
    }

    private void writeProperties(ImmutableObjectType type) {
        for (Property prop : type.getProperties().values()) {
            handleDtoProp(prop, () -> {
                writeProperty(prop);
            });
        }
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
                writeFields((ImmutableObjectType) type);
                writeProperties((ImmutableObjectType) type);
                writeTargetTypeDeclarations((ImmutableObjectType) type);
            });
            code('\n');
        }
    }
}
