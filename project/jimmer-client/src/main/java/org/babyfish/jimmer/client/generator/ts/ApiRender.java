package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.runtime.Service;
import org.babyfish.jimmer.client.source.Source;
import org.babyfish.jimmer.impl.util.StringUtil;

import java.util.List;
import java.util.stream.Collectors;

public class ApiRender implements Render {

    private final String name;

    private final List<Service> services;

    public ApiRender(String name, List<Service> services) {
        this.name = name;
        this.services = services;
    }

    @Override
    public void export(SourceWriter writer) {
        writer.code("export {").code(name).code("} from './").code(name).code("';\n");
    }

    @Override
    public void render(SourceWriter writer) {

        TypeScriptContext ctx = writer.getContext();
        List<Source> serviceSources = services
                .stream()
                .map(ctx::getSource)
                .collect(Collectors.toList());

        Source executorSource = ctx.getRootSource("Executor");
        writer.importSource(executorSource);
        for (Source source : serviceSources) {
            writer.importSource(source, true);
        }

        writer.code("export class ").code(name).code(' ');
        writer.scope(SourceWriter.ScopeType.OBJECT, "", true, () -> {
            for (Source source : serviceSources) {
                writer
                        .code("\nreadonly ")
                        .code(StringUtil.identifier(source.getName()))
                        .code(": ")
                        .code(source.getName())
                        .code('\n');
            }
            writer.code("\nconstructor(executor: ").code(executorSource.getName()).code(") ");
            writer.scope(SourceWriter.ScopeType.OBJECT, "", true, () -> {
                for (Source source : serviceSources) {
                    writer
                            .code("this.")
                            .code(StringUtil.identifier(source.getName()))
                            .code(" = new ")
                            .code(source.getName())
                            .code("(executor);\n");
                }
            });
        });
    }
}
