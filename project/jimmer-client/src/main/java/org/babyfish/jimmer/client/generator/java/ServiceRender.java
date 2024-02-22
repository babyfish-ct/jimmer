package org.babyfish.jimmer.client.generator.java;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServiceRender implements Render {

    private final Service service;

    public ServiceRender(Service service) {
        this.service = service;
    }

    @Override
    public void render(SourceWriter writer) {
        writer.code("public interface ").code(service.getJavaType().getSimpleName()).code(' ');
        writer.scope(CodeWriter.ScopeType.OBJECT, "", true, writer::renderChildren).code('\n');
    }
}
