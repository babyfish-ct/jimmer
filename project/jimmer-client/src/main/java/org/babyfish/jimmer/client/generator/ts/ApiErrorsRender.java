package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.runtime.*;
import org.babyfish.jimmer.client.source.Source;
import org.babyfish.jimmer.impl.util.StringUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ApiErrorsRender implements Render {

    private final String name;

    private final List<Service> services;

    private final Set<ObjectType> usedExceptionTypes;

    public ApiErrorsRender(String name, List<Service> services) {
        this.name = name;
        this.services = services;
        Set<ObjectType> exceptionTypes = new LinkedHashSet<>();
        for (Service service : services) {
            for (Operation operation : service.getOperations()) {
                exceptionTypes.addAll(operation.getExceptionTypes());
            }
        }
        this.usedExceptionTypes = exceptionTypes;
    }

    @Override
    public void export(SourceWriter writer) {
        writer.code("export type {AllErrors, ").code(name).code("} from './").code(name).code("';\n");
    }

    @Override
    public void render(SourceWriter writer) {
        renderAllErrors(writer);
        renderApiErrors(writer);
    }

    private void renderAllErrors(SourceWriter writer) {
        writer.code("export type AllErrors = ");
        if (usedExceptionTypes.isEmpty()) {
            writer.code("{};\n");
            return;
        }
        writer.scope(SourceWriter.ScopeType.BLANK, " | ", false, () -> {
            for (ObjectType exceptionType : usedExceptionTypes) {
                TypeDefinition.Error error = exceptionType.getError();
                assert error != null;
                writer.separator();
                writer.scope(SourceWriter.ScopeType.OBJECT, ", ", true, () -> {
                    writer.code("family: '").code(error.getFamily()).code('\'');
                    writer.separator();
                    writer.code("code: '").code(error.getCode()).code('\'');
                    for (Property property : exceptionType.getProperties().values()) {
                        writer.separator();
                        writer
                                .doc(property.getDoc())
                                .code(property.getName())
                                .codeIf(property.getType() instanceof NullableType, '?')
                                .code(": ")
                                .typeRef(property.getType());
                    }
                });
            }
        });
        writer.code(";\n");
    }

    private void renderApiErrors(SourceWriter writer) {
        TypeScriptContext ctx = writer.getContext();
        writer.code("export type ").code(name).code(" = ");
        if (usedExceptionTypes.isEmpty()) {
            writer.code("{};\n");
            return;
        }
        writer.scope(SourceWriter.ScopeType.OBJECT, ", ", true, () -> {
            for (Service service : services) {
                Source serviceSource = ctx.getSource(service);
                writer.separator();
                writer.code("'").code(StringUtil.identifier(serviceSource.getName())).code("': ");
                writer.scope(SourceWriter.ScopeType.OBJECT, ", ", true, () -> {
                    for (Operation operation : service.getOperations()) {
                        if (operation.getExceptionTypes().isEmpty()) {
                            continue;
                        }
                        writer.separator();
                        Source operationSource = ctx.getSource(operation);
                        writer.code('\'').code(operationSource.getName()).code("': AllErrors & ");
                        writer.scope(SourceWriter.ScopeType.ARGUMENTS, " | ", false, () -> {
                            for (ObjectType exceptionType : operation.getExceptionTypes()) {
                                TypeDefinition.Error error = exceptionType.getError();
                                assert error != null;
                                writer.separator();
                                writer.scope(SourceWriter.ScopeType.OBJECT, ", ", true, () -> {
                                    writer.code("family: '").code(error.getFamily()).code('\'');
                                    writer.separator();
                                    writer.code("code: '").code(error.getCode()).code('\'');
                                    writer.separator();
                                    writer.code("readonly [key:string]: any");
                                });
                            }
                        });
                    }
                });
            }
        });
        writer.code(";\n");
    }
}
