package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.generator.SourceAwareRender;
import org.babyfish.jimmer.client.runtime.*;
import org.babyfish.jimmer.client.runtime.impl.ImmutableObjectTypeImpl;
import org.babyfish.jimmer.client.source.Source;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class FetchedTypeRender implements SourceAwareRender {

    private final String name;

    private final ObjectType objectType;

    private Map<Type, String> recursiveTypeNames;

    public FetchedTypeRender(String name, ObjectType objectType) {
        this.name = name;
        this.objectType = objectType;
    }

    @Override
    public void initialize(Source source) {
        Source parentSource = source.getParent();
        assert parentSource != null;
        this.recursiveTypeNames = ((DtoWrapperRender)parentSource.getRender()).recursiveTypeNames;
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
        FetchByInfo info = objectType.getFetchByInfo();
        assert info != null;
        writer.code('\'').code(name).code("': ");
        render(objectType, writer, recursiveTypeNames);
        writer.code('\n');
    }

    static void render(ObjectType type, CodeWriter writer, Map<Type, String> recursiveTypeNames) {
        TypeScriptContext ctx = writer.getContext();
        writer.scope(CodeWriter.ScopeType.OBJECT, "", true, () -> {
            for (Property property : type.getProperties().values()) {
                writer
                        .codeIf(ctx.isMutable(), "readonly ")
                        .code(property.getName())
                        .codeIf(property.getType() instanceof NullableType, '?')
                        .code(": ");
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
                if (targetType instanceof ImmutableObjectTypeImpl) {
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
