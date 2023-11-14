package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.File;
import org.babyfish.jimmer.client.generator.Generator;
import org.babyfish.jimmer.client.generator.GeneratorException;
import org.babyfish.jimmer.client.generator.ts.simple.*;
import org.babyfish.jimmer.client.meta.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TypeScriptGenerator implements Generator {

    private final String moduleName;

    private final int indent;

    private final boolean anonymous;

    private final boolean mutable;

    public TypeScriptGenerator() {
        this("Api", 4, false, false);
    }

    public TypeScriptGenerator(String moduleName) {
        this(moduleName, 4, false, false);
    }

    public TypeScriptGenerator(String moduleName, int indent) {
        this(moduleName, indent, false, false);
    }

    public TypeScriptGenerator(String moduleName, int indent, boolean anonymous, boolean mutable) {
        this.mutable = mutable;
        if (moduleName == null || moduleName.isEmpty()) {
            moduleName = "Api";
        } else if ("All".equals(moduleName)) {
            throw new IllegalArgumentException("modueName cannot be `All`");
        }
        this.moduleName = moduleName;
        this.indent = indent;
        this.anonymous = anonymous;
    }

    @Override
    public void generate(Metadata metadata, OutputStream out) {
        try {
            ZipOutputStream zipOut = new ZipOutputStream(out);
            generate0(new TsContext(metadata, zipOut, moduleName, indent, anonymous), zipOut);
            zipOut.finish();
        } catch (IOException | RuntimeException | java.lang.Error ex) {
            throw new GeneratorException(ex);
        }
    }

    private void generate0(TsContext ctx, ZipOutputStream zipOut) throws IOException {

        putEntry(zipOut, ctx.getModuleFile());
        new ModuleWriter(ctx).flush();
        zipOut.closeEntry();

        putEntry(zipOut, ctx.getModuleErrorsFile());
        new ModuleErrorsWriter(ctx).flush();
        zipOut.closeEntry();

        putEntry(zipOut, ExecutorWriter.FILE);
        new ExecutorWriter(ctx).flush();
        zipOut.closeEntry();

        putEntry(zipOut, DynamicWriter.FILE);
        new DynamicWriter(ctx, mutable).flush();
        zipOut.closeEntry();

        putEntry(zipOut, RequestOfWriter.FILE);
        new RequestOfWriter(ctx).flush();
        zipOut.closeEntry();

        putEntry(zipOut, ResponseOfWriter.FILE);
        new ResponseOfWriter(ctx).flush();
        zipOut.closeEntry();

        putEntry(zipOut, ElementOfWriter.FILE);
        new ElementOfWriter(ctx).flush();
        zipOut.closeEntry();

        Map<String, Index> indexMap = new HashMap<>();
        for (Map.Entry<Service, File> e : ctx.getServiceFileMap().entrySet()) {
            Service service = e.getKey();
            File file = e.getValue();
            indexMap.computeIfAbsent(file.getDir(), Index::new).addObjectFile(file);
            putEntry(zipOut, file);
            new ServiceWriter(ctx, service, mutable).flush();
            zipOut.closeEntry();
        }
        for (Map.Entry<Type, File> e : ctx.getTypeFilePairs()) {
            Type type = e.getKey();
            File file = e.getValue();
            indexMap.computeIfAbsent(file.getDir(), Index::new).addTypeFile(file);
            putEntry(zipOut, file);
            new TypeDefinitionWriter(ctx, type, mutable).flush();
            zipOut.closeEntry();
        }
        if (!anonymous) {
            for (Map.Entry<Class<?>, List<ImmutableObjectType>> e : ctx.getDtoMap().entrySet()) {
                Class<?> rawType = e.getKey();
                DtoWriter dtoWriter = new DtoWriter(ctx, rawType, mutable);
                indexMap.computeIfAbsent(dtoWriter.getFile().getDir(), Index::new).addTypeFile(dtoWriter.getFile());
                putEntry(zipOut, dtoWriter.getFile());
                dtoWriter.flush();
                zipOut.closeEntry();
            }
        }

        indexMap.computeIfAbsent("", Index::new)
                .addObjectFile(ctx.getModuleFile())
                .addTypeFile(ctx.getModuleErrorsFile())
                .addTypeFile(ExecutorWriter.FILE)
                .addTypeFile(DynamicWriter.FILE)
                .addTypeFile(RequestOfWriter.FILE)
                .addTypeFile(ResponseOfWriter.FILE)
                .addTypeFile(ElementOfWriter.FILE);
        for (Index index : indexMap.values()) {
            writeIndex(ctx, index, zipOut);
        }
    }

    private void writeIndex(Context ctx, Index index, ZipOutputStream zipOut) throws IOException {
        putEntry(zipOut, index.dir + "/index.ts");
        OutputStreamWriter writer = new OutputStreamWriter(zipOut);
        for (File file : index.objectFiles) {
            writer.write(
                    "export { " +
                            file.getName() +
                            " } from './" +
                            file.getName() +
                            "';\n"
            );
            if (!anonymous && file != ctx.getModuleFile()) {
                writer.write(
                        "export type { " +
                                file.getName() +
                                "Options } from './" +
                                file.getName() +
                                "';\n"
                );
            }
        }
        for (File file : index.typeFiles) {
            if (file == ctx.getModuleErrorsFile()) {
                writer.write(
                        "export type { " +
                                file.getName() +
                                ", AllErrors } from './" +
                                file.getName() +
                                "';\n"
                );
            } else {
                writer.write(
                        "export type { " +
                                file.getName() +
                                " } from './" +
                                file.getName() +
                                "';\n"
                );
            }
        }
        writer.flush();
        zipOut.closeEntry();
    }

    private static void putEntry(ZipOutputStream zipOut, File file) throws IOException {
        putEntry(zipOut, file + ".ts");
    }

    private static void putEntry(ZipOutputStream zipOut, String fileName) throws IOException {
        if (fileName.charAt(0) == '/') {
            fileName = fileName.substring(1);
        }
        zipOut.putNextEntry(new ZipEntry(fileName));
    }

    private static class Index {

        final String dir;

        final Set<File> objectFiles = new TreeSet<>();

        final Set<File> typeFiles = new TreeSet<>();

        public Index(String dir) {
            this.dir = dir;
        }

        public Index addObjectFile(File file) {
            objectFiles.add(file);
            return this;
        }

        public Index addTypeFile(File file) {
            typeFiles.add(file);
            return this;
        }
    }
}
