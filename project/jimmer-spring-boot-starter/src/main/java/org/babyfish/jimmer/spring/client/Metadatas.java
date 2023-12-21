package org.babyfish.jimmer.spring.client;

import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.runtime.Operation;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Metadatas {

    private static final Pattern COMMA_PATTERN = Pattern.compile("\\s*,\\s*");

    private Metadatas() {}

    public static Metadata create(String apiName, int indent, @Nullable String groups) {
        return Metadata
                .newBuilder()
                .setOperationParser(new OperationParserImpl())
                .setParameterParameter(new ParameterParserImpl())
                .setGenericSupported(true)
                .setGroups(
                        groups != null && !groups.isEmpty() ?
                                Arrays.asList(COMMA_PATTERN.split(groups)) :
                                null
                )
                .build();
    }

    private static class OperationParserImpl implements Metadata.OperationParser {

        @Override
        public String uri(AnnotatedElement element) {
            RequestMapping requestMapping = element.getAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                String uri = uri(requestMapping.value(), requestMapping.path());
                if (uri != null) {
                    return uri;
                }
            }
            GetMapping getMapping = element.getAnnotation(GetMapping.class);
            if (getMapping != null) {
                String uri = uri(getMapping.value(), getMapping.path());
                if (uri != null) {
                    return uri;
                }
            }
            PostMapping postMapping = element.getAnnotation(PostMapping.class);
            if (postMapping != null) {
                String uri = uri(postMapping.value(), postMapping.path());
                if (uri != null) {
                    return uri;
                }
            }
            PutMapping putMapping = element.getAnnotation(PutMapping.class);
            if (putMapping != null) {
                String uri = uri(putMapping.value(), putMapping.path());
                if (uri != null) {
                    return uri;
                }
            }
            DeleteMapping deleteMapping = element.getAnnotation(DeleteMapping.class);
            if (deleteMapping != null) {
                String uri = uri(deleteMapping.value(), deleteMapping.path());
                if (uri != null) {
                    return uri;
                }
            }
            return null;
        }

        @Override
        public Operation.HttpMethod http(Method method) {
            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                if (requestMapping.method().length == 0) {
                    return Operation.HttpMethod.GET;
                }
                switch (requestMapping.method()[0]) {
                    case HEAD: return Operation.HttpMethod.HEAD;
                    case POST: return Operation.HttpMethod.POST;
                    case PUT: return Operation.HttpMethod.PUT;
                    case PATCH: return Operation.HttpMethod.PATCH;
                    case DELETE: return Operation.HttpMethod.DELETE;
                    case OPTIONS: return Operation.HttpMethod.OPTIONS;
                    case TRACE: return Operation.HttpMethod.TRACE;
                    default: return Operation.HttpMethod.GET;
                }
            }
            if (method.getAnnotation(PostMapping.class) != null) {
                return Operation.HttpMethod.POST;
            }
            if (method.getAnnotation(PutMapping.class) != null) {
                return Operation.HttpMethod.PUT;
            }
            if (method.getAnnotation(DeleteMapping.class) != null) {
                return Operation.HttpMethod.DELETE;
            }
            return Operation.HttpMethod.GET;
        }

        private static String uri(String[] value, String[] path) {
            for (String uri : value) {
                if (!uri.isEmpty()) {
                    return uri;
                }
            }
            for (String uri : path) {
                if (!uri.isEmpty()) {
                    return uri;
                }
            }
            return null;
        }
    }

    private static class ParameterParserImpl implements Metadata.ParameterParser {

        @Nullable
        @Override
        public String requestParam(Parameter javaParameter) {
            RequestParam requestParam = javaParameter.getAnnotation(RequestParam.class);
            if (requestParam == null) {
                return null;
            }
            String name = requestParam.value();
            if (name.isEmpty()) {
                name = requestParam.name();
            }
            return name;
        }

        @Override
        public boolean isDefault(Parameter javaParameter) {
            RequestParam requestParam = javaParameter.getAnnotation(RequestParam.class);
            return requestParam != null && !requestParam.defaultValue().isEmpty();
        }

        @Nullable
        @Override
        public String pathVariable(Parameter javaParameter) {
            PathVariable pathVariable = javaParameter.getAnnotation(PathVariable.class);
            if (pathVariable == null) {
                return null;
            }
            String name = pathVariable.value();
            if (name.isEmpty()) {
                name = pathVariable.name();
            }
            return name;
        }

        @Override
        public boolean isRequestBody(Parameter javaParameter) {
            return javaParameter.isAnnotationPresent(RequestBody.class);
        }
    }
}
