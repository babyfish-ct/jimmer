package org.babyfish.jimmer.client.generator.java.feign;

import org.babyfish.jimmer.client.generator.java.JavaCodeWriter;
import org.babyfish.jimmer.client.generator.java.JavaContext;
import org.babyfish.jimmer.client.meta.NullableType;
import org.babyfish.jimmer.client.meta.Operation;
import org.babyfish.jimmer.client.meta.Parameter;
import org.babyfish.jimmer.client.meta.Service;

import java.io.IOException;
import java.io.Writer;

public class ServiceWriter extends JavaCodeWriter<JavaContext> {

    private static final String GET_MAPPING = "org.springframework.web.bind.annotation.GetMapping";

    private static final String POST_MAPPING = "org.springframework.web.bind.annotation.PostMapping";

    private static final String PUT_MAPPING = "org.springframework.web.bind.annotation.PutMapping";

    private static final String DELETE_MAPPING = "org.springframework.web.bind.annotation.DeleteMapping";

    private static final String REQUEST_PARAM = "org.springframework.web.bind.annotation.RequestParam";

    private static final String PATH_VARIABLE = "org.springframework.web.bind.annotation.PathVariable";

    private static final String REQUEST_BODY = "org.springframework.web.bind.annotation.RequestBody";

    private final Service service;

    public ServiceWriter(JavaContext ctx, Service service) {
        super(ctx, ctx.getFile(service));
        this.service = service;
    }

    @Override
    protected void write() {

        importType("org.springframework.cloud.openfeign.FeignClient");

        code('\n');
        document(service.getDocument());
        code("@FeignClient(name = \"").code(ctx.getModuleFile().getName()).code("\")\n");
        code("public interface ").code(file.getName()).code(' ');
        scope(ScopeType.OBJECT, "", true, () -> {
            for (Operation operation : service.getOperations()) {
                writeOperation(operation);
            }
        });
    }

    private void writeOperation(Operation operation) {
        code('\n');
        document(operation.getDocument());
        String annotation;
        switch (operation.getHttpMethod()) {
            case POST:
                annotation = POST_MAPPING;
                break;
            case PUT:
                annotation = PUT_MAPPING;
                break;
            case DELETE:
                annotation = DELETE_MAPPING;
                break;
            default:
                annotation = GET_MAPPING;
        }
        code('@');
        typeRef(annotation).code("(\"").code(operation.getUri()).code("\")\n");
        typeRef(operation.getType())
                .code(' ')
                .code(ctx.getOperationName(operation));
        scope(ScopeType.ARGUMENTS, ", ", operation.getParameters().size() > 2, () -> {
            for (Parameter parameter : operation.getParameters()) {
                separator();
                if (parameter.getRequestParam() != null) {
                    code('@');
                    typeRef(REQUEST_PARAM)
                            .code("(name = \"")
                            .code(parameter.getRequestParam())
                            .code("\"");
                    if (parameter.getType() instanceof NullableType) {
                        code(", required = false");
                    }
                    code(") ");
                } else if (parameter.getPathVariable() != null) {
                    code('@');
                    typeRef(PATH_VARIABLE).code("(\"").code(parameter.getPathVariable()).code("\") ");
                } else if (parameter.isRequestBody()) {
                    code('@');
                    typeRef(REQUEST_BODY).code(' ');
                }
                typeRef(parameter.getType()).code(' ').code(parameter.getName());
            }
        });
        code(";\n");
    }
}
