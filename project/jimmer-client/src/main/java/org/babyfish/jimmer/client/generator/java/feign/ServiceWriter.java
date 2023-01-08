package org.babyfish.jimmer.client.generator.java.feign;

import org.babyfish.jimmer.client.generator.java.JavaCodeWriter;
import org.babyfish.jimmer.client.generator.ts.File;
import org.babyfish.jimmer.client.meta.Service;

import java.io.IOException;
import java.io.Writer;

public class ServiceWriter extends JavaCodeWriter<FeignContext> {

    private final Service service;

    protected ServiceWriter(FeignContext ctx, File file, Service service) {
        super(ctx, file);
        this.service = service;
    }

    @Override
    protected void write() {
        document(service.getDocument());
    }

    @Override
    protected void writeImportHeader(Writer writer) throws IOException {
        writer.write("import org.springframework.cloud.openfeign.FeignClient;\n");
        super.writeImportHeader(writer);
    }
}
