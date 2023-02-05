package org.babyfish.jimmer.client.generator.java;

import org.babyfish.jimmer.client.meta.*;

import java.lang.reflect.TypeVariable;

public class TypeDefinitionWriter extends JavaCodeWriter<JavaContext> {

    private final Type type;

    public TypeDefinitionWriter(JavaContext ctx, Type type) {
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
            writeObjectType((ObjectType) type);
        } else {
            writeEnumType();
        }
    }

    private void writeObjectType(ObjectType objectType) {
        code("\npublic class ")
                .code(
                        objectType instanceof StaticObjectType ?
                                objectType.getJavaType().getSimpleName() :
                                file.getName()
                );
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
            for (Property prop : objectType.getProperties().values()) {
                writeField(prop, objectType instanceof ImmutableObjectType);
            }
            for (Property prop : objectType.getProperties().values()) {
                writeProperty(prop, objectType instanceof ImmutableObjectType);
            }
            if (objectType instanceof StaticObjectType) {
                for (StaticObjectType nestedType : ((StaticObjectType) objectType).getNestedTypes()) {
                    code("\n");
                    writeObjectType(nestedType);
                }
            }
        });
        code('\n');
    }

    private void writeEnumType() {
        EnumType enumType = (EnumType)type;
        code("\npublic enum ").code(getFile().getName()).code(' ');
        scope(ScopeType.OBJECT, ", ", true, () -> {
            for (String item : enumType.getItems()) {
                separator();
                code(item);
            }
        });
        code("\n");
    }
}
