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

    public TypeScriptGenerator() {
        this("Api", 4, false);
    }

    public TypeScriptGenerator(String moduleName) {
        this(moduleName, 4, false);
    }

    public TypeScriptGenerator(String moduleName, int indent) {
        this(moduleName, indent, false);
    }

    public TypeScriptGenerator(String moduleName, int indent, boolean anonymous) {
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

        zipOut.putNextEntry(new ZipEntry(ctx.getModuleFile() + ".ts"));
        new ModuleWriter(ctx).flush();
        zipOut.closeEntry();

        zipOut.putNextEntry(new ZipEntry(ctx.getModuleErrorsFile() + ".ts"));
        new ModuleErrorsWriter(ctx).flush();
        zipOut.closeEntry();

        zipOut.putNextEntry(new ZipEntry(ExecutorWriter.FILE + ".ts"));
        new ExecutorWriter(ctx).flush();
        zipOut.closeEntry();

        zipOut.putNextEntry(new ZipEntry(DynamicWriter.FILE + ".ts"));
        new DynamicWriter(ctx).flush();
        zipOut.closeEntry();

        zipOut.putNextEntry(new ZipEntry(RequestOfWriter.FILE + ".ts"));
        new RequestOfWriter(ctx).flush();
        zipOut.closeEntry();

        zipOut.putNextEntry(new ZipEntry(ResponseOfWriter.FILE + ".ts"));
        new ResponseOfWriter(ctx).flush();
        zipOut.closeEntry();

        zipOut.putNextEntry(new ZipEntry(ElementOfWriter.FILE + ".ts"));
        new ElementOfWriter(ctx).flush();
        zipOut.closeEntry();

        Map<String, Index> indexMap = new HashMap<>();
        for (Map.Entry<Service, File> e : ctx.getServiceFileMap().entrySet()) {
            Service service = e.getKey();
            File file = e.getValue();
            indexMap.computeIfAbsent(file.getDir(), Index::new).addObjectFile(file);
            zipOut.putNextEntry(new ZipEntry(file + ".ts"));
            new ServiceWriter(ctx, service).flush();
            zipOut.closeEntry();
        }
        for (Map.Entry<Type, File> e : ctx.getTypeFilePairs()) {
            Type type = e.getKey();
            File file = e.getValue();
            indexMap.computeIfAbsent(file.getDir(), Index::new).addTypeFile(file);
            zipOut.putNextEntry(new ZipEntry(file + ".ts"));
            new TypeDefinitionWriter(ctx, type).flush();
            zipOut.closeEntry();
        }
        if (!anonymous) {
            for (Map.Entry<Class<?>, List<ImmutableObjectType>> e : ctx.getDtoMap().entrySet()) {
                Class<?> rawType = e.getKey();
                DtoWriter dtoWriter = new DtoWriter(ctx, rawType);
                indexMap.computeIfAbsent(dtoWriter.getFile().getDir(), Index::new).addTypeFile(dtoWriter.getFile());
                zipOut.putNextEntry(new ZipEntry(dtoWriter.getFile() + ".ts"));
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
        zipOut.putNextEntry(new ZipEntry(index.dir + "/index.ts"));
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

    private static class Index {

        final String dir;

        final List<File> objectFiles = new ArrayList<>();

        final List<File> typeFiles = new ArrayList<>();

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
