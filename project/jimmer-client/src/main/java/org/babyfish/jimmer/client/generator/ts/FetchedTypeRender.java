package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.runtime.*;
import org.babyfish.jimmer.client.runtime.impl.FetchedTypeImpl;

import java.util.Map;

public class FetchedTypeRender implements Render {

    private final String name;

    private final ObjectType objectType;

    final Map<Type, String> recursiveTypeNames;

    public FetchedTypeRender(String name, ObjectType objectType, Map<Type, String> recursiveTypeNames) {
        this.name = name;
        this.objectType = objectType;
        this.recursiveTypeNames = recursiveTypeNames;
        collectRecursiveTypeNames(objectType);
    }

    private void collectRecursiveTypeNames(ObjectType type) {
        if (type.isRecursiveFetchedType()) {
            if (recursiveTypeNames.containsKey(type)) {
                return;
            }
            recursiveTypeNames.put(type, "RecursiveType_" + (recursiveTypeNames.size() + 1));
        }
        for (Property property : type.getProperties().values()) {
            Type targetType = property.getType();
            boolean isNullable = targetType instanceof NullableType;
            if (isNullable) {
                targetType = ((NullableType)targetType).getTargetType();
            }
            boolean isList = targetType instanceof ListType;
            if (isList) {
                targetType = ((ListType)targetType).getElementType();
            }
            if (targetType instanceof ObjectType) {
                collectRecursiveTypeNames((ObjectType) targetType);
            }
        }
    }

    @Override
    public void render(CodeWriter writer) {
        assert objectType.getFetchByInfo() != null;
        Doc doc = objectType.getFetchByInfo().getDoc();
        if (doc == null) {
            doc = objectType.getDoc();
        }
        writer.doc(doc);
        writer.code('\'').code(name).code("': ");
        render(objectType, writer, recursiveTypeNames);
        writer.code('\n');
    }

    static void render(ObjectType type, CodeWriter writer, Map<Type, String> recursiveTypeNames) {
        TypeScriptContext ctx = writer.getContext();
        writer.scope(CodeWriter.ScopeType.OBJECT, "", true, () -> {
            for (Property property : type.getProperties().values()) {
                writer
                        .doc(property.getDoc())
                        .codeIf(!ctx.isMutable(), "readonly ")
                        .code(property.getName());
                Type targetType = property.getType();
                boolean isNullable = targetType instanceof NullableType;
                if (isNullable) {
                    targetType = ((NullableType)targetType).getTargetType();
                }
                boolean isList = targetType instanceof ListType;
                if (isList) {
                    targetType = ((ListType)targetType).getElementType();
                }
                String recursiveTypeName = recursiveTypeNames.get(targetType);
                writer.codeIf(property.getType() instanceof NullableType || recursiveTypeName != null, '?')
                        .code(": ");
                if (targetType instanceof FetchedTypeImpl) {
                    ObjectType targetObjectType = (ObjectType) targetType;
                    writeResolvedType(writer, isNullable, isList, () -> {
                        if (recursiveTypeName != null) {
                            writer.code(recursiveTypeName);
                        } else {
                            render(targetObjectType, writer, recursiveTypeNames);
                        }
                    });
                } else {
                    writer.typeRef(property.getType());
                }
                writer.codeIf(recursiveTypeName != null, " | null | undefined");
                writer.code(";\n");
            }
        });
    }

    private static void writeResolvedType(CodeWriter writer, boolean isNullable, boolean isList, Runnable block) {
        TypeScriptContext ctx = writer.getContext();
        if (isList) {
            writer.code(ctx.isMutable() ? "Array<" : "ReadonlyArray<");
        }
        block.run();
        if (isList) {
            writer.code('>');
        }
        if (isNullable) {
            writer.code(" | null | undefined");
        }
    }
}
