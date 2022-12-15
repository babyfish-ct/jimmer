package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.client.generator.Generator;
import org.babyfish.jimmer.client.generator.GeneratorException;
import org.babyfish.jimmer.client.meta.ImmutableObjectType;
import org.babyfish.jimmer.client.meta.Metadata;
import org.babyfish.jimmer.client.meta.Service;
import org.babyfish.jimmer.client.meta.Type;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TypeScriptGenerator implements Generator {

    private final String moduleName;

    private final int indent;

    public TypeScriptGenerator() {
        this.moduleName = "Api";
        this.indent = 4;
    }

    public TypeScriptGenerator(String moduleName) {
        this.moduleName = moduleName;
        this.indent = 4;
    }

    public TypeScriptGenerator(String moduleName, int indent) {
        this.moduleName = moduleName;
        this.indent = indent;
    }

    @Override
    public void generate(Metadata metadata, OutputStream out) {
        try {
            ZipOutputStream zipOut = new ZipOutputStream(out);
            generate0(new Context(metadata, zipOut, moduleName, indent), zipOut);
            zipOut.finish();
        } catch (IOException | RuntimeException | Error ex) {
            throw new GeneratorException(ex);
        }
    }

    private void generate0(Context ctx, ZipOutputStream zipOut) throws IOException {

        zipOut.putNextEntry(new ZipEntry(ctx.getModuleFile().toString()));
        new ModuleWriter(ctx).flush();
        zipOut.closeEntry();

        zipOut.putNextEntry(new ZipEntry(ExecutorWriter.FILE.toString()));
        new ExecutorWriter(ctx).flush();
        zipOut.closeEntry();

        zipOut.putNextEntry(new ZipEntry(DynamicWriter.FILE.toString()));
        new DynamicWriter(ctx).flush();
        zipOut.closeEntry();

        Map<String, Index> indexMap = new HashMap<>();
        for (Map.Entry<Service, File> e : ctx.getServiceFileMap().entrySet()) {
            Service service = e.getKey();
            File file = e.getValue();
            indexMap.computeIfAbsent(file.getDir(), Index::new).addServiceFile(file);
            zipOut.putNextEntry(new ZipEntry(file.toString()));
            new ServiceWriter(ctx, service).flush();
            zipOut.closeEntry();
        }
        for (Map.Entry<Type, File> e : ctx.getTypeFilePairs()) {
            Type type = e.getKey();
            File file = e.getValue();
            indexMap.computeIfAbsent(file.getDir(), Index::new).addTypeFile(file);
            zipOut.putNextEntry(new ZipEntry(file.toString()));
            new TypeDefinitionWriter(ctx, type).flush();
            zipOut.closeEntry();
        }
        for (Map.Entry<Class<?>, List<ImmutableObjectType>> e : ctx.getDtoMap().entrySet()) {
            Class<?> rawType = e.getKey();
            DtoWriter dtoWriter = new DtoWriter(ctx, rawType, e.getValue());
            indexMap.computeIfAbsent(dtoWriter.getFile().getDir(), Index::new).addTypeFile(dtoWriter.getFile());
            zipOut.putNextEntry(new ZipEntry(dtoWriter.getFile().toString()));
            dtoWriter.flush();
            zipOut.closeEntry();
        }

        indexMap.computeIfAbsent("", Index::new)
                .addServiceFile(ctx.getModuleFile())
                .addTypeFile(ExecutorWriter.FILE)
                .addTypeFile(DynamicWriter.FILE);
        for (Index index : indexMap.values()) {
            writeIndex(index, zipOut);
        }
    }

    private void writeIndex(Index index, ZipOutputStream zipOut) throws IOException {
        zipOut.putNextEntry(new ZipEntry(index.dir + "/index.ts"));
        OutputStreamWriter writer = new OutputStreamWriter(zipOut);
        for (File file : index.serviceFiles) {
            writer.write(
                    "export { " +
                            file.getName() +
                            " } from './" +
                            file.getName() +
                            "';\n"
                    );
        }
        for (File file : index.typeFiles) {
            writer.write(
                    "export type { " +
                            file.getName() +
                            " } from './" +
                            file.getName() +
                            "';\n"
            );
        }
        writer.flush();
        zipOut.closeEntry();
    }

    private static class Index {

        final String dir;

        final List<File> serviceFiles = new ArrayList<>();

        final List<File> typeFiles = new ArrayList<>();

        public Index(String dir) {
            this.dir = dir;
        }

        public Index addServiceFile(File file) {
            serviceFiles.add(file);
            return this;
        }

        public Index addTypeFile(File file) {
            typeFiles.add(file);
            return this;
        }
    }
}
