package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.runtime.NullableType;
import org.babyfish.jimmer.client.runtime.Operation;
import org.babyfish.jimmer.client.runtime.Parameter;
import org.babyfish.jimmer.client.runtime.Service;
import org.babyfish.jimmer.client.runtime.impl.NullableTypeImpl;

public class ServiceRender implements Render {

    private final String name;

    private final Service service;

    public ServiceRender(String name, Service service) {
        this.name = name;
        this.service = service;
    }

    @Override
    public void export(SourceWriter writer) {
        writer.code("export {").code(name).code("} from './").code(name).code("';\n");
    }

    @Override
    public void render(SourceWriter writer) {
        renderService(writer);
        renderOptions(writer);
    }

    private void renderService(SourceWriter writer) {
        writer.importSource(writer.getContext().getRootSource("Executor"));
        writer.doc(service.getDoc()).code("export class ").code(name).code(' ');
        writer.scope(SourceWriter.ScopeType.OBJECT, "", true, () -> {
            writer.code("\nconstructor(private executor: Executor) {}\n");
            writer.renderChildren();
        });
        writer.code('\n');
    }

    private void renderOptions(SourceWriter writer) {
        TypeScriptContext ctx = writer.getContext();
        writer.code("export type ").code(name).code("Options = ");
        writer.scope(SourceWriter.ScopeType.OBJECT, ", ", true, () -> {
            for (Operation operation : service.getOperations()) {
                if (operation.getParameters().isEmpty()) {
                    continue;
                }
                writer.separator().code('\'').code(ctx.getSource(operation).getName()).code("': ");
                writer.scope(
                        SourceWriter.ScopeType.OBJECT,
                        ", ",
                        true,
                        () -> {
                            Doc doc = operation.getDoc();
                            for (Parameter parameter : operation.getParameters()) {
                                writer.separator();
                                if (doc != null) {
                                    writer.doc(doc.getParameterValueMap().get(parameter.getName()));
                                }
                                writer
                                        .codeIf(!ctx.isMutable(), "readonly ")
                                        .code(parameter.getName())
                                        .codeIf(parameter.getType() instanceof NullableType || parameter.getDefaultValue() != null, '?')
                                        .code(": ")
                                        .typeRef(
                                                parameter.getDefaultValue() != null ?
                                                        NullableTypeImpl.of(parameter.getType()) :
                                                        parameter.getType()
                                        );
                            }
                        }
                );
            }
        });
        writer.code('\n');
    }
}
