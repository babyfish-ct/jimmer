package org.babyfish.jimmer.client.generator.java;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.runtime.*;
import org.babyfish.jimmer.client.runtime.impl.IllegalApiException;
import org.babyfish.jimmer.client.source.Source;
import org.babyfish.jimmer.impl.util.Classes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class JavaWriter extends SourceWriter {

    private static final Set<Class<?>> ADDITIONAL_TYPES = new HashSet<>(
            Arrays.asList(Nullable.class, NotNull.class, List.class, Map.class)
    );

    private final Map<Type, String> typeNameMap = new LinkedHashMap<>();

    private final Map<Class<?>, String> classNameMap = new LinkedHashMap<>();

    private final Map<String, String> importMap = new LinkedHashMap<>();

    public JavaWriter(Context ctx, Source source) {
        super(ctx, source);
    }

    public SourceWriter typeRef(Class<?> type) {
        String name = classNameMap.get(type);
        if (name != null) {
            code(name);
            return this;
        }
        if (!ADDITIONAL_TYPES.contains(type)) {
            throw new IllegalApiException("\"" + type + "\" is not additional type of java writer");
        }
        if (importMap.containsKey(type.getSimpleName())) {
            classNameMap.put(type, type.getName());
            code(type.getName());
        } else {
            importMap.put(type.getSimpleName(), type.getName());
            classNameMap.put(type, type.getSimpleName());
            code(type.getSimpleName());
        }
        return this;
    }

    @Override
    public SourceWriter typeRef(Type type) {
        if (type instanceof TypeVariable) {
            code(((TypeVariable)type).getName());
        } else if (type instanceof NullableType) {
            Type targetType = ((NullableType)type).getTargetType();
            if (targetType instanceof SimpleType) {
                Class<?> targetJavaType = ((SimpleType)targetType).getJavaType();
                if (targetJavaType.isPrimitive()) {
                    code(Classes.boxTypeOf(targetJavaType).getSimpleName());
                    return this;
                }
            }
            typeRef(targetType);
        } else if (type instanceof ListType) {
            typeRef(List.class).code('<').typeRef(((ListType)type).getElementType()).code('>');
        } else if (type instanceof MapType) {
            typeRef(Map.class).code("<String, ").typeRef(((MapType)type).getValueType()).code('>');
        } else if (type instanceof EnumType) {
            code(typeName((EnumType) type));
        } else if (type instanceof VirtualType) {
            if (type instanceof VirtualType.File) {
                code("java.io.InputStream");
            } else {
                throw new AssertionError("Internal bug: more virtual type need to be processed");
            }
        } else if (type instanceof SimpleType) {
            Class<?> javaType = ((SimpleType)type).getJavaType();
            if (javaType.getPackage() != null &&
                    javaType.getPackage().getName().equals("java.lang") &&
                    javaType.getDeclaringClass() == null) {
                code(javaType.getSimpleName());
            } else {
                code(javaType.getName());
            }
        } else if (type instanceof ObjectType) {
            ObjectType objectType = (ObjectType) type;
            code(typeName(objectType));
            if (objectType.getKind() == ObjectType.Kind.STATIC) {
                List<Type> arguments = ((ObjectType) type).getArguments();
                if (!arguments.isEmpty()) {
                    scope(ScopeType.GENERIC, ", ", false, () -> {
                        for (Type argument : arguments) {
                            separator();
                            typeRef(argument);
                        }
                    });
                }
            }
        }
        return this;
    }

    @Override
    public void importSource(Source source, String name, boolean treatAsValue) {
    }

    @Override
    protected void onFlushImportedTypes() {
        String sp = "package ";
        for (String dir : source.getDirs()) {
            code(sp).code(dir);
            sp = ".";
        }
        code(";\n");
        if (!importMap.isEmpty()) {
            code('\n');
            for (String path : new TreeSet<>(importMap.values())) {
                code("import ").code(path).code(";\n");
            }
        }
    }

    private String typeName(NamedType type) {
        String typeName = typeNameMap.get(type);
        if (typeName != null) {
            return typeName;
        }
        Source source = getSource(type);
        String shortName = source.getName();
        if (source.getDirs().isEmpty() && type.getSimpleNames().size() == 1) {
            typeNameMap.put(type, shortName);
            return shortName;
        }
        StringBuilder builder = new StringBuilder();
        for (String dir : source.getDirs()) {
            if (builder.length() != 0) {
                builder.append('.');
            }
            builder.append(dir);
        };
        for (String simpleName : type.getSimpleNames()) {
            builder.append('.').append(simpleName);
        }
        String qualifiedName = builder.toString();
        if (importMap.containsKey(shortName)) {
            typeNameMap.put(type, qualifiedName);
            return qualifiedName;
        }
        importMap.put(shortName, qualifiedName);
        typeNameMap.put(type, shortName);
        return shortName;
    }
}
