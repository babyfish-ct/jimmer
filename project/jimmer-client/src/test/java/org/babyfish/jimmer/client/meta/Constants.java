package org.babyfish.jimmer.client.meta;

import org.babyfish.jimmer.client.java.service.AuthorService;
import org.babyfish.jimmer.client.java.service.BookService;
import org.babyfish.jimmer.client.kotlin.service.KBookService;
import org.babyfish.jimmer.client.meta.common.*;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;

public class Constants {

    private static final Metadata.OperationParser OPERATION_PARSER = methodMethod -> {
        GetMapping getMapping = methodMethod.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            return new Tuple2<>(getMapping.value(), Operation.HttpMethod.GET);
        }
        PutMapping putMapping = methodMethod.getAnnotation(PutMapping.class);
        if (putMapping != null) {
            return new Tuple2<>(putMapping.value(), Operation.HttpMethod.PUT);
        }
        DeleteMapping deleteMapping = methodMethod.getAnnotation(DeleteMapping.class);
        if (deleteMapping != null) {
            return new Tuple2<>(deleteMapping.value(), Operation.HttpMethod.DELETE);
        }
        RequestMapping requestMapping = methodMethod.getAnnotation(RequestMapping.class);
        if (requestMapping != null) {
            return new Tuple2<>(requestMapping.value(), requestMapping.method().length == 0 ? null : requestMapping.method()[0]);
        }
        return null;
    };

    private static final Metadata.ParameterParser PARAMETER_PARSER = new Metadata.ParameterParser() {

        @Nullable
        @Override
        public Tuple2<String, Boolean> requestParamNameAndNullable(Parameter javaParameter) {
            RequestParam requestParam = javaParameter.getAnnotation(RequestParam.class);
            return requestParam != null ? new Tuple2<>(requestParam.value(), !requestParam.required()) : null;
        }

        @Nullable
        @Override
        public String pathVariableName(Parameter javaParameter) {
            PathVariable pathVariable = javaParameter.getAnnotation(PathVariable.class);
            return pathVariable != null ? pathVariable.value() : null;
        }

        @Override
        public boolean isRequestBody(Parameter javaParameter) {
            return javaParameter.isAnnotationPresent(RequestBody.class);
        }
    };

    public final static Metadata JAVA_METADATA = Metadata.newBuilder()
            .addServiceType(BookService.class)
            .addServiceType(AuthorService.class)
            .setOperationParser(
                    OPERATION_PARSER
            )
            .setParameterParser(
                    PARAMETER_PARSER
            )
            .build();

    public final static Metadata KOTLIN_METADATA = Metadata.newBuilder()
            .addServiceType(KBookService.class)
            .setOperationParser(
                    OPERATION_PARSER
            )
            .setParameterParser(
                    PARAMETER_PARSER
            )
            .build();
}
