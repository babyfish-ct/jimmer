package org.babyfish.jimmer.client.generator.java;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.ts.DtoWriter;
import org.babyfish.jimmer.client.generator.File;
import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.impl.util.Classes;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public abstract class JavaCodeWriter<C extends JavaContext> extends CodeWriter<C> {

    private static final Set<String> PRIMITIVE_TYPE_NAMES;

    private final Set<String> importedPaths = new TreeSet<>();

    private Property currentDtoProp;

    protected JavaCodeWriter(C ctx, File file) {
        super(ctx, file);
    }

    @Override
    public void onImport(File file, boolean treatAsData, /* no use for java */ List<String> nestedNames) {
        if (!file.getDir().equals(this.file.getDir())) {
            StringBuilder builder = new StringBuilder();
            if (!ctx.getBasePackage().isEmpty()) {
                builder.append(ctx.getBasePackage());
                builder.append('.');
            }
            builder.append(file.toString().replace('/', '.'));
            importedPaths.add(builder.toString());
        }
    }

    @Override
    protected void writePackageHeader(Writer writer) throws IOException {
        writer.write("package ");
        writer.write(getContext().getBasePackage());
        if (!ctx.getBasePackage().isEmpty() && !file.getDir().isEmpty()) {
            writer.write('.');
        }
        if (!file.getDir().isEmpty()) {
            writer.write(file.getDir().replace('/', '.'));
        }
        writer.write(";\n");
    }

    @Override
    protected void writeImportHeader(Writer writer) throws IOException {

        if (!importedPaths.isEmpty()) {
            writer.write('\n');
            for (String typeName : importedPaths) {
                writer.write("import ");
                writer.write(typeName);
                writer.write(";\n");
            }
        }
    }

    protected final void importType(String typeName) {
        if (!PRIMITIVE_TYPE_NAMES.contains(typeName)) {
            int lastDotIndex = typeName.lastIndexOf('.');
            if (lastDotIndex == -1 || !typeName.substring(0, lastDotIndex).equals("java.lang")) {
                importedPaths.add(typeName);
            }
        }
    }

    protected final void importType(Class<?> type) {
        importType(type.getName());
    }

    @Override
    protected void writeSimpleTypeRef(SimpleType simpleType) {
        Class<?> javaType = simpleType.getJavaType();
        code(javaType.getSimpleName());
        importType(javaType);
    }

    @Override
    protected void writeNullableTypeRef(NullableType nullableType) {
        typeRef(nullableType.getTargetType());
    }

    @Override
    protected void writeArrayTypeRef(ArrayType arrayType) {
        code("List<").typeRef(arrayType.getElementType()).code('>');
        importType(List.class);
    }

    @Override
    protected void writeMapTypeRef(MapType mapType) {
        code("Map<")
                .typeRef(mapType.getKeyType())
                .code(", ")
                .typeRef(mapType.getValueType())
                .code('>');
        importType(Map.class);
    }

    @Override
    protected void writeDynamicTypeRef(ImmutableObjectType type) {
        code("Dynamic_").code(getContext().getFile(type).getName());
    }

    @Override
    protected void writeDtoTypeRef(ImmutableObjectType type) {
        String tempDtoTypeName = tempDtoTypeName();
        if (tempDtoTypeName != null) {
            code(tempDtoTypeName);
        } else {
            importFile(DtoWriter.dtoFile(getContext(), type.getJavaType()));
            code(getContext().getDtoPrefix(type.getJavaType()))
                    .code('.')
                    .code(getContext().getDtoSuffix(type).replace('/', '_'));
        }
    }

    protected JavaCodeWriter<C> typeRef(String typeName) {
        int lastDotIndex = typeName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            code(typeName);
        } else {
            code(typeName.substring(lastDotIndex + 1));
        }
        importType(typeName);
        return this;
    }

    protected final void writeField(Property prop) {
        writeField(prop, false);
    }

    protected final void writeField(Property prop, boolean forceNullable) {
        code('\n');
        document(prop.getDocument());
        if (forceNullable || prop.getType() instanceof NullableType) {
            code("@Nullable\n");
            importType(Nullable.class);
        }
        code("private ");
        writePropType(prop, forceNullable);
        code(' ').code(prop.getName()).code(";\n");
    }

    protected final void writeProperty(Property prop) {
        writeProperty(prop, false);
    }

    protected final void writeProperty(Property prop, boolean forceNullable) {
        if (forceNullable || prop.getType() instanceof NullableType) {
            code("\n@Nullable");
            importType(Nullable.class);
        }
        code("\npublic ");
        writePropType(prop, forceNullable);
        code(' ').code(getterName(prop)).code("() ");
        scope(ScopeType.OBJECT, "", true, () -> {
            code("return ").code(prop.getName()).code(";\n");
        });
        code('\n');

        code("\npublic void ");
        code(setterName(prop))
                .code('(')
                .codeIf(forceNullable || prop.getType() instanceof NullableType, "@Nullable ");
        writePropType(prop, forceNullable);
        code(' ')
                .code(prop.getName())
                .code(") ");
        scope(ScopeType.OBJECT, "", true, () -> {
            code("this.").code(prop.getName()).code(" = ").code(prop.getName()).code(";\n");
        });
        code('\n');
    }

    private void writePropType(Property prop, boolean forceNullable) {
        Type type = prop.getType();
        if (forceNullable && type instanceof SimpleType) {
            Class<?> javaType = ((SimpleType)type).getJavaType();
            if (javaType.isPrimitive()) {
                code(Classes.boxTypeOf(javaType).getSimpleName());
                return;
            }
        }
        typeRef(prop.getType());
    }

    private static String getterName(Property property) {
        String name = property.getName();
        boolean isBoolean = property.getType() instanceof SimpleType &&
                ((SimpleType)property.getType()).getJavaType() == boolean.class;
        if (isBoolean) {
            if (name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2))) {
                return name;
            }
        }
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return isBoolean ? "is" + name : "get" + name;
    }

    private static String setterName(Property property) {
        String name = property.getName();
        boolean isBoolean = property.getType() instanceof SimpleType &&
                ((SimpleType)property.getType()).getJavaType() == boolean.class;
        if (isBoolean) {
            if (name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2))) {
                name = name.substring(2);
            }
        }
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return "set" + name;
    }

    protected void handleDtoProp(Property prop, Runnable handler) {
        Property oldProp = currentDtoProp;
        currentDtoProp = prop;
        try {
            handler.run();
        } finally {
            currentDtoProp = oldProp;
        }
    }

    protected String tempDtoTypeName() {
        if (currentDtoProp == null) {
            return null;
        }
        return "TargetOf_" + currentDtoProp.getName();
    }

    static {
        Set<String> set = new HashSet<>();
        set.add(boolean.class.getSimpleName());
        set.add(char.class.getSimpleName());
        set.add(byte.class.getSimpleName());
        set.add(short.class.getSimpleName());
        set.add(int.class.getSimpleName());
        set.add(long.class.getSimpleName());
        set.add(float.class.getSimpleName());
        set.add(double.class.getSimpleName());
        PRIMITIVE_TYPE_NAMES = set;
    }
}
