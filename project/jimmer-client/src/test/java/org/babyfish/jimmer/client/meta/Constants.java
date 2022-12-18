package org.babyfish.jimmer.client.meta;

import org.babyfish.jimmer.client.service.*;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;

public class Constants {

    public final static Metadata METADATA = Metadata.newBuilder()
            .addServiceType(BookService.class)
            .addServiceType(AuthorService.class)
            .setOperationParser(
                    javaMethod -> {
                        GetMapping getMapping = javaMethod.getAnnotation(GetMapping.class);
                        if (getMapping != null) {
                            return new Tuple2<>(getMapping.value(), Operation.HttpMethod.GET);
                        }
                        PutMapping putMapping = javaMethod.getAnnotation(PutMapping.class);
                        if (putMapping != null) {
                            return new Tuple2<>(putMapping.value(), Operation.HttpMethod.PUT);
                        }
                        DeleteMapping deleteMapping = javaMethod.getAnnotation(DeleteMapping.class);
                        if (deleteMapping != null) {
                            return new Tuple2<>(deleteMapping.value(), Operation.HttpMethod.DELETE);
                        }
                        return null;
                    }
            )
            .setParameterParser(
                    new Metadata.ParameterParser() {

                        @Nullable
                        @Override
                        public Tuple2<String, Boolean> requestParamNameAndNullable(java.lang.reflect.Parameter javaParameter) {
                            RequestParam requestParam = javaParameter.getAnnotation(RequestParam.class);
                            return requestParam != null ? new Tuple2<>(requestParam.value(), !requestParam.required()) : null;
                        }

                        @Nullable
                        @Override
                        public String pathVariableName(java.lang.reflect.Parameter javaParameter) {
                            PathVariable pathVariable = javaParameter.getAnnotation(PathVariable.class);
                            return pathVariable != null ? pathVariable.value() : null;
                        }

                        @Override
                        public boolean isRequestBody(Parameter javaParameter) {
                            return javaParameter.isAnnotationPresent(RequestBody.class);
                        }
                    }
            )
            .build();
}
