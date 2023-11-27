package org.babyfish.jimmer.client.meta;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KFunction;
import kotlin.reflect.KType;
import org.babyfish.jimmer.client.java.service.*;
import org.babyfish.jimmer.client.kotlin.service.*;
import org.babyfish.jimmer.client.meta.common.*;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.lang.reflect.Parameter;

public class Constants {

    private static final Metadata.OperationParser OPERATION_PARSER =
            new Metadata.OperationParser() {
                @Override
                public Tuple2<String, Operation.HttpMethod> http(AnnotatedElement method) {
                    GetMapping getMapping = method.getAnnotation(GetMapping.class);
                    if (getMapping != null) {
                        return new Tuple2<>(getMapping.value(), Operation.HttpMethod.GET);
                    }
                    PutMapping putMapping = method.getAnnotation(PutMapping.class);
                    if (putMapping != null) {
                        return new Tuple2<>(putMapping.value(), Operation.HttpMethod.PUT);
                    }
                    DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
                    if (deleteMapping != null) {
                        return new Tuple2<>(deleteMapping.value(), Operation.HttpMethod.DELETE);
                    }
                    PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
                    if (patchMapping != null) {
                        return new Tuple2<>(patchMapping.value(), Operation.HttpMethod.PATCH);
                    }
                    RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                    if (requestMapping != null) {
                        return new Tuple2<>(requestMapping.value(), requestMapping.method().length == 0 ? null : requestMapping.method()[0]);
                    }
                    return null;
                }

                @Override
                public String[] getParameterNames(Method method) {
                    if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == FindBookArguments.class) {
                        return new String[] { "arguments" };
                    }
                    return null;
                }

                @Override
                public KType kotlinType(KFunction<?> function) {
                    KType type = function.getReturnType();
                    if (JvmClassMappingKt.getKotlinClass(ResponseEntity.class).equals(type.getClassifier())) {
                        return type.getArguments().get(0).getType();
                    }
                    return type;
                }

                @Override
                public AnnotatedType javaType(Method method) {
                    if (method.getReturnType() == ResponseEntity.class) {
                        return ((AnnotatedParameterizedType)method.getAnnotatedReturnType()).getAnnotatedActualTypeArguments()[0];
                    }
                    return method.getAnnotatedReturnType();
                }
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

        @Override
        public boolean shouldBeIgnored(Parameter javaParameter) {
            return false;
        }
    };

    public final static Metadata JAVA_METADATA = Metadata.newBuilder()
            .addServiceType(BookService.class)
            .addServiceType(AuthorService.class)
            .addServiceType(ArrayService.class)
            .addServiceType(EnumService.class)
            .addServiceType(NotApiService.class)
            .setOperationParser(
                    OPERATION_PARSER
            )
            .setParameterParser(
                    PARAMETER_PARSER
            )
            .build();

    public final static Metadata KOTLIN_METADATA = Metadata.newBuilder()
            .addServiceType(KBookService.class)
            .addServiceType(KBookStoreService.class)
            .addServiceType(KArrayService.class)
            .addServiceType(KEnumService.class)
            .addServiceType(KNotApiService.class)
            .setOperationParser(
                    OPERATION_PARSER
            )
            .setParameterParser(
                    PARAMETER_PARSER
            )
            .build();
}
