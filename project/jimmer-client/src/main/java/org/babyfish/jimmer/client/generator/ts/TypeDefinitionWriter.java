package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.meta.*;

import java.lang.reflect.TypeVariable;

public class TypeDefinitionWriter extends CodeWriter {

    private final Type type;

    public TypeDefinitionWriter(Context ctx, Type type) {
        super(ctx, ctx.getFile(type));
        if (!type.hasDefinition()) {
            throw new IllegalArgumentException("The type does not have definition");
        }
        this.type = type;
    }

    @Override
    protected void write() {
        if (type instanceof ObjectType) {
            document(((ObjectType)type).getDocument());
            writeObjectType();
        } else {
            writeEnumType();
        }
    }

    private void writeObjectType() {
        ObjectType objectType = (ObjectType) type;
        code("export interface ").code(getFile().getName());
        if (objectType instanceof StaticObjectType) {
            StaticObjectType staticObjectType = (StaticObjectType) objectType;
            TypeVariable<? extends Class<?>>[] typeParameters = staticObjectType.getJavaType().getTypeParameters();
            if (typeParameters.length != 0 && staticObjectType.getTypeArguments().isEmpty()) {
                scope(ScopeType.GENERIC, ", ", false, () -> {
                    for (TypeVariable<?> typeVariable : typeParameters) {
                        separator();
                        code(typeVariable.getName());
                    }
                });
            }
        }
        code(' ');
        scope(ScopeType.OBJECT, "", true, () -> {
            for (Property property : objectType.getProperties().values()) {
                separator();
                code('\n');
                document(property.getDocument());
                code("readonly ")
                        .code(property.getName())
                        .codeIf(property.getType() instanceof NullableType, "?")
                        .code(": ");
                type(NullableType.unwrap(property.getType()));
                code(';');
            }
        });
    }

    private void writeEnumType() {
        EnumType enumType = (EnumType)type;
        code("export type ").code(getFile().getName()).code(" = ");
        scope(ScopeType.BLANK, " | ", enumType.getItems().size() > 3, () -> {
            for (String item : enumType.getItems()) {
                separator();
                code('\'').code(item).code('\'');
            }
        });
        code(";\n");
    }
}
