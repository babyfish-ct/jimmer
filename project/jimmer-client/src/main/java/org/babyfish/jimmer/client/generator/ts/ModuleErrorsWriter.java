package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.File;
import org.babyfish.jimmer.client.meta.EnumBasedError;
import org.babyfish.jimmer.client.meta.NullableType;
import org.babyfish.jimmer.client.meta.Operation;
import org.babyfish.jimmer.client.meta.Service;

import java.util.*;

public class ModuleErrorsWriter extends TsCodeWriter {

    // Family -> Errors
    private final Map<String, Set<EnumBasedError>> errorMap;

    public ModuleErrorsWriter(TsContext ctx) {
        super(ctx, ctx.getModuleErrorsFile());
        Map<String, Set<EnumBasedError>> errorMap = new TreeMap<>();
        for (Service service : ctx.getServiceFileMap().keySet()) {
            for (Operation operation : service.getOperations()) {
                for (EnumBasedError error : operation.getErrors()) {
                    String family = error.getRawError().getClass().getSimpleName();
                    errorMap
                            .computeIfAbsent(
                                    family,
                                    f -> new TreeSet<EnumBasedError>(Comparator.comparing(it -> it.getRawError().name()))
                            )
                            .add(error);
                }
            }
        }
        this.errorMap = Collections.unmodifiableMap(errorMap);
    }

    @Override
    protected void write() {
        writeAllErrors();
        writeModuleErrors();
    }

    private void writeAllErrors() {
        if (this.errorMap.isEmpty()) {
            code("export type AllErrors = {}");
        } else {
            code("export type AllErrors = ");
            scope(ScopeType.BLANK, " | ", true, () -> {
                for (Map.Entry<String, Set<EnumBasedError>> e : errorMap.entrySet()) {
                    for (EnumBasedError error : e.getValue()) {
                        separator();
                        writeError(error);
                    }
                }
            });
        }
        code(";\n");
    }

    private void writeError(EnumBasedError error) {
        scope(ScopeType.OBJECT, ",", true, () -> {
            separator();
            code("readonly family: \"").code(error.getRawError().getClass().getSimpleName()).code('"');
            separator();
            code("readonly code: \"").code(error.getRawError().name()).code('"');
            for (EnumBasedError.Field field : error.getFields().values()) {
                separator();
                writeField(field);
            }
        });
    }

    private void writeField(EnumBasedError.Field field) {
        code("readonly \"").code(field.getName()).code('"');
        if (field.getType() instanceof NullableType) {
            code('?');
        }
        code(": ");
        typeRef(field.getType());
    }

    private void writeModuleErrors() {
        code("\nexport type ").code(ctx.getModuleFile().getName()).code("Errors = ");
        scope(ScopeType.OBJECT, ",", true, () -> {
            for (Map.Entry<Service, File> e : ctx.getServiceFileMap().entrySet()) {
                separator();
                code('"').code(toFieldName(e.getValue().getName())).code("\": ");
                scope(ScopeType.OBJECT, ",", true, () -> {
                    for (Operation operation : e.getKey().getOperations()) {
                        if (!operation.getErrors().isEmpty()) {
                            separator();
                            code('"').code(ctx.getOperationName(operation)).code("\": AllErrors & ");
                            scope(ScopeType.ARGUMENTS, " | ", true, () -> {
                                for (EnumBasedError error : operation.getErrors()) {
                                    separator();
                                    scope(ScopeType.OBJECT, ",", true, () -> {
                                        code("readonly family: ")
                                                .code('\'')
                                                .code(error.getRawError().getClass().getSimpleName())
                                                .code('\'');
                                        separator();
                                        code("readonly code: ").code('\'').code(error.getRawError().name()).code('\'');
                                        separator();
                                        code("readonly [key:string]: any");
                                    });
                                }
                            });
                        }
                    }
                });
            }
        });
        code(";\n");
    }

    private static String toFieldName(String serviceName) {
        char[] chs = serviceName.toCharArray();
        for (int i = 0; i < chs.length; i++) {
            if (Character.isUpperCase(chs[i])) {
                chs[i] = Character.toLowerCase(chs[i]);
            } else {
                break;
            }
        }
        return new String(chs);
    }
}
