package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.Generator;
import org.babyfish.jimmer.client.generator.GeneratorException;
import org.babyfish.jimmer.client.meta.Metadata;
import org.babyfish.jimmer.client.meta.Service;
import org.babyfish.jimmer.client.meta.Type;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TypeScriptGenerator implements Generator {

    private final String indent;

    public TypeScriptGenerator() {
        this.indent = "    ";
    }

    public TypeScriptGenerator(String indent) {
        this.indent = indent;
    }

    @Override
    public void generate(Metadata metadata, OutputStream out) {
        try {
            ZipOutputStream zipOut = new ZipOutputStream(out);
            generate0(new Context(metadata, zipOut, indent), zipOut);
            zipOut.finish();
        } catch (IOException | RuntimeException | Error ex) {
            throw new GeneratorException(ex);
        }
    }

    private void generate0(Context ctx, ZipOutputStream zipOut) throws IOException {
        for (Map.Entry<Service, File> e : ctx.getServiceFileMap().entrySet()) {
            zipOut.putNextEntry(new ZipEntry(e.getValue().toString()));
            new ServiceWriter(ctx, e.getKey(), e.getValue()).write();
            zipOut.closeEntry();
        }
        for (Map.Entry<Type, File> e : ctx.getTypeFileMap().entrySet()) {
            zipOut.putNextEntry(new ZipEntry(e.getValue().toString()));
            zipOut.closeEntry();
        }
    }
}
