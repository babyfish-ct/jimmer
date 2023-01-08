package org.babyfish.jimmer.client.generator.java.feign;

import org.babyfish.jimmer.client.generator.Generator;
import org.babyfish.jimmer.client.generator.GeneratorException;
import org.babyfish.jimmer.client.generator.java.DtoWriter;
import org.babyfish.jimmer.client.generator.java.JavaContext;
import org.babyfish.jimmer.client.generator.File;
import org.babyfish.jimmer.client.generator.java.TypeDefinitionWriter;
import org.babyfish.jimmer.client.meta.ImmutableObjectType;
import org.babyfish.jimmer.client.meta.Metadata;
import org.babyfish.jimmer.client.meta.Service;
import org.babyfish.jimmer.client.meta.Type;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class JavaFeignGenerator implements Generator {

    private final String feignClientName;

    private final int indent;

    private final String basePackage;

    public JavaFeignGenerator(String feignClientName, int indent, String basePackage) {
        this.feignClientName = feignClientName;
        this.indent = indent;
        this.basePackage = basePackage;
    }

    @Override
    public void generate(Metadata metadata, OutputStream out) {
        try {
            ZipOutputStream zipOut = new ZipOutputStream(out);
            generate0(new JavaContext(metadata, zipOut, feignClientName, indent, basePackage), zipOut);
            zipOut.finish();
        } catch (IOException | RuntimeException | Error ex) {
            throw new GeneratorException(ex);
        }
    }

    private void generate0(JavaContext ctx, ZipOutputStream zipOut) throws IOException {
        String baseDir = ctx.getBasePackage().isEmpty() ?
                "" :
                ctx.getBasePackage().replace('.', '/') + '/';
        for (Map.Entry<Service, File> e : ctx.getServiceFileMap().entrySet()) {
            Service service = e.getKey();
            File file = e.getValue();
            zipOut.putNextEntry(new ZipEntry(baseDir + file + ".java"));
            new ServiceWriter(ctx, service).flush();
            zipOut.closeEntry();
        }
        for (Map.Entry<Type, File> e : ctx.getTypeFilePairs()) {
            Type type = e.getKey();
            File file = e.getValue();
            zipOut.putNextEntry(new ZipEntry(baseDir + file + ".java"));
            new TypeDefinitionWriter(ctx, type).flush();
            zipOut.closeEntry();
        }
        for (Map.Entry<Class<?>, List<ImmutableObjectType>> e : ctx.getDtoMap().entrySet()) {
            Class<?> rawType = e.getKey();
            DtoWriter dtoWriter = new DtoWriter(ctx, rawType);
            zipOut.putNextEntry(new ZipEntry(baseDir + dtoWriter.getFile() + ".java"));
            dtoWriter.flush();
            zipOut.closeEntry();
        }
    }
}
