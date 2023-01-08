package org.babyfish.jimmer.client.generator.java;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.ts.DtoWriter;
import org.babyfish.jimmer.client.generator.ts.File;
import org.babyfish.jimmer.client.meta.*;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public abstract class JavaCodeWriter<C extends JavaContext> extends CodeWriter<C> {

    private final Set<Class<?>> importedTypes = new LinkedHashSet<>();

    private final Set<File> importedFiles = new LinkedHashSet<>();

    private Property currentDtoProp;

    protected JavaCodeWriter(C ctx, File file) {
        super(ctx, file);
    }

    @Override
    public void onImport(File file, boolean treatAsData) {
        importedFiles.add(file);
    }

    @Override
    protected void writePackageHeader(Writer writer) throws IOException {
        writer.write("package ");
        writer.write(getContext().getBasePackage());
        if (!file.getDir().isEmpty()) {
            writer.write('.');
            writer.write(file.getDir().replace('/', '.'));
        }
        writer.write(";\n");
    }

    @Override
    protected void writeImportHeader(Writer writer) throws IOException {

        if (!importedTypes.isEmpty()) {
            writer.write('\n');
            for (Class<?> importedType : importedTypes) {
                writer.write("import ");
                writer.write(importedType.getName());
                writer.write(";\n");
            }
        }

        if (!importedFiles.isEmpty()) {
            writer.write('\n');
            for (File importedFile : importedFiles) {
                writer.write(getContext().getBasePackage());
                if (importedFile.getDir().isEmpty()) {
                    writer.write('.');
                    writer.write(importedFile.getDir().replace('/', '.'));
                }
                writer.write('.');
                writer.write(importedFile.getName());
                writer.write('\n');
            }
        }
    }

    protected void importType(Class<?> type) {
        if (type.getPackage() != null && !type.getPackage().getName().equals("java.lang")) {
            importedTypes.add(type);
        }
    }

    @Override
    protected void writeSimpleTypeRef(SimpleType simpleType) {
        Class<?> javaType = simpleType.getJavaType();
        code(javaType.getSimpleName());
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
                    .code(getContext().getDtoSuffix(type).replace('/', '.'));
        }
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
}
