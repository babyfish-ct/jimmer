package org.babyfish.jimmer.client.generator.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.client.generator.Namespace;
import org.babyfish.jimmer.client.runtime.*;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

public class OpenApiGenerator {

    private final Metadata metadata;

    private final Map<String, Object> headers;

    private final ServiceNameManager serviceNameManager = new ServiceNameManager();

    private final OperationNameManager operationNameManager = new OperationNameManager();

    private final FetcherContext fetcherContext = new FetcherContext();

    private final TypeNameManager typeNameManager = new TypeNameManager(fetcherContext);

    private final TypeRefManager typeRefManager = new TypeRefManager(typeNameManager);

    public OpenApiGenerator(Metadata metadata,Map<String, Object> headers) {
        this.metadata = metadata;
        this.headers = headers;
    }

    public void generate( Writer writer) throws IOException {
        Map<String, Object> finalMap = new LinkedHashMap<>();
        if (headers != null) {
            finalMap.putAll(headers);
        }
        finalMap.put("paths", generateOperations());
        new ObjectMapper().writeValue(writer, finalMap);
    }

    private Map<String, Object> generateOperations() {
        Map<String, Object> pathMap = new LinkedHashMap<>();
        for (Service service : metadata.getServices()) {
            for (Operation operation : service.getOperations()) {
                pathMap.put(operation.getUri(), genericOperation(service, operation));
            }
        }
        return pathMap;
    }

    private Object genericOperation(Service service, Operation operation) {
        Map<String, Object> op = new LinkedHashMap<>();
        Map<String, Object> detail = new LinkedHashMap<>();
        op.put(operation.getHttpMethod().name().toLowerCase(), detail);
        detail.put("tags", new String[] {serviceNameManager.get(service)});
        detail.put("operationId", operationNameManager.get(operation));
        List<Object> parameters = new ArrayList<>(operation.getParameters().size());
        detail.put("parameters", parameters);
        if (!operation.getParameters().isEmpty()) {
            for (Parameter parameter : operation.getParameters()) {
                String name = parameter.getRequestParam();
                if (name == null) {
                    name = parameter.getPathVariable();
                }
                if (name != null) {
                    parameters.add(
                            generateParam(
                                    name,
                                    parameter.getType(),
                                    parameter.getRequestParam() != null ? "query" : "path",
                                    parameter.getDefaultValue()
                            )
                    );
                    continue;
                }
                if (parameter.isRequestBody()) {
                    detail.put("requestBody", generateRequestBody(operation, parameter));
                }
                if (parameter.getType() instanceof ObjectType) {
                    for (Property property : ((ObjectType)parameter.getType()).getProperties().values()) {
                        parameters.add(
                                generateParam(
                                        property.getName(),
                                        property.getType(),
                                        "query",
                                        null
                                )
                        );
                    }
                }
            }
        }
        Map<Integer, List<Response>> responseMap = new TreeMap<>();
        responseMap.put(
                200,
                Collections.singletonList(
                        new Response(
                                operation.getReturnType(),
                                operation.getDoc() != null ?
                                        operation.getDoc().getReturnValue() :
                                        null
                        )
                )
        );
        for (ObjectType exceptionType : operation.getExceptionTypes()) {
            responseMap.computeIfAbsent(500, it -> new ArrayList<>())
                    .add(
                            new Response(
                                    exceptionType,
                                    operation.getDoc() != null ?
                                            operation.getDoc().getReturnValue() :
                                            null
                            )
                    );
        }
        op.put("responses", generateResponses(responseMap));
        return op;
    }

    private Object generateParam(String name, Type type, String in, String defaultValue) {
        Map<String, Object> param = new LinkedHashMap<>();
        if (name != null) {
            param.put("name", name);
            param.put("in", in);
            param.put("required", type instanceof NullableType);
        }
        Map<String, Object> schema = typeRefManager.get(type);
        if (defaultValue != null) {
            schema = new LinkedHashMap<>(schema);
            if (type instanceof SimpleType) {
                Class<?> javaType = ((SimpleType)type).getJavaType();
                if (boolean.class == javaType) {
                    schema.put("default", Boolean.parseBoolean(defaultValue));
                } else if (float.class == javaType) {
                    schema.put("default", Float.parseFloat(defaultValue));
                } else if (double.class == javaType) {
                    schema.put("default", Double.parseDouble(defaultValue));
                } else if (long.class == javaType) {
                    schema.put("default", Long.parseLong(defaultValue));
                } else if (javaType.isPrimitive() && javaType != char.class) {
                    schema.put("default", Integer.parseInt(defaultValue));
                } else {
                    schema.put("default", defaultValue);
                }
            } else {
                schema.put("default", defaultValue);
            }
        }
        param.put("schema", schema);
        return param;
    }

    private Object generateRequestBody(Operation operation, Parameter parameter) {
        Map<String, Object> requestBody = new LinkedHashMap<>(3);
        if (operation.getDoc() != null) {
            requestBody.put("description", operation.getDoc().getParameterValueMap().get(parameter.getName()));
        }
        Map<String, Object> content = new LinkedHashMap<>();
        requestBody.put("content", content);
        Object schema = generateParam(null, parameter.getType(), null, null);
        content.put("application/json", schema);
        return requestBody;
    }

    private Object generateResponses(Map<Integer, List<Response>> responseMap) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<Response>> e : responseMap.entrySet()) {
            int httpStatus = e.getKey();
            List<Response> responses = e.getValue();
            Map<String, Object> subMap = new LinkedHashMap<>();
            if (httpStatus == 200) {
                subMap.put("description", responses.get(0).doc != null ? responses.get(0).doc : "OK");
            } else {
                subMap.put("description", "ERROR");
            }
            Map<String, Object> type = new LinkedHashMap<>();
            if (responses.size() == 1) {
                type.put("$ref", typeRefManager.get(responses.get(0).type));
            } else {
                List<Object> types = new ArrayList<>();
                for (Response response : responses) {
                    types.add(
                            Collections.singletonMap(
                                    "$ref",
                                    typeRefManager.get(response.type)
                            )
                    );
                }
                type.put("oneOf", types);
            }
            Map<String, Object> content = new LinkedHashMap<>();
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("schema", type);
            content.put("*/*", schema);
            subMap.put("content", content);
            map.put(Integer.toString(httpStatus), subMap);
        }
        return map;
    }

    private static class ServiceNameManager {

        private final Map<Service, String> nameMap = new HashMap<>();

        private final Namespace namespace = new Namespace();

        public String get(Service service) {
            return nameMap.computeIfAbsent(service, it -> namespace.allocate(it.getJavaType().getSimpleName()));
        }
    }

    private static class OperationNameManager {

        private final Map<Operation, String> nameMap = new HashMap<>();

        private final Namespace namespace = new Namespace();

        public String get(Operation operation) {
            return nameMap.computeIfAbsent(operation, it -> namespace.allocate(it.getName()));
        }
    }

    private static class FetcherContext {

        private final LinkedList<FetchByInfo> fetchByInfoStack = new LinkedList<>();

        public void fetch(FetchByInfo info, Runnable block) {
            if (info == null) {
                block.run();
            } else {
                fetchByInfoStack.push(info);
                try {
                    block.run();
                } finally {
                    fetchByInfoStack.pop();
                }
            }
        }

        public FetchByInfo fetchByInfo() {
            return fetchByInfoStack.pop();
        }
    }

    private static class TypeNameManager {

        private final FetcherContext ctx;

        private final Map<Type, Map<String, Object>> typeMapMap = new HashMap<>();

        private final Namespace namespace = new Namespace();

        private TypeNameManager(FetcherContext ctx) {
            this.ctx = ctx;
        }

        public String get(ObjectType type) {
            return getImpl(type);
        }

        private String getImpl(Type type) {
            if (type instanceof ObjectType) {
                ObjectType objectType = (ObjectType) type;
                StringBuilder builder = new StringBuilder();
                builder.append(String.join("_", objectType.getSimpleNames()));
                FetchByInfo info = null;
                if (objectType.getKind() == ObjectType.Kind.FETCHED) {
                    info = objectType.getFetchByInfo();
                    if (info == null) {
                        info = ctx.fetchByInfo();
                    }
                }
                if (info != null) {
                    builder.append('_').append(info.getOwnerType().getSimpleName())
                            .append('_').append(info.getConstant());
                }
                for (Type argument : objectType.getArguments()) {
                    builder.append('_').append(getImpl(argument));
                }
                return namespace.allocate(builder.toString());
            } else if (type instanceof NullableType) {
                return getImpl(((NullableType) type).getTargetType());
            } else if (type instanceof ListType) {
                return "List_" + getImpl(((ListType) type).getElementType());
            } else if (type instanceof MapType) {
                throw new UnsupportedOperationException("TODO");
            } else if (type instanceof EnumType) {
                return String.join("_", ((EnumType)type).getSimpleNames());
            } else {
                return ((SimpleType)type).getJavaType().getSimpleName();
            }
        }
    }

    private static class TypeRefManager {

        private final TypeNameManager typeNameManager;

        private final Map<Type, Map<String, Object>> typeMap = new HashMap<>();

        private TypeRefManager(TypeNameManager typeNameManager) {
            this.typeNameManager = typeNameManager;
        }

        public Map<String, Object> get(Type type) {
            return typeMap.computeIfAbsent(type, this::create);
        }

        private Map<String, Object> create(Type type) {
            if (type instanceof ObjectType) {
                return Collections.singletonMap(
                        "$ref",
                        "#/components/schemas/" + typeNameManager.get((ObjectType) type)
                );
            } else if (type instanceof ListType) {
                Map<String, Object> map = new LinkedHashMap<>(3);
                map.put("type", "array");
                map.put("items", create(((ListType)type).getElementType()));
                return Collections.unmodifiableMap(map);
            } else if (type instanceof MapType) {
                throw new UnsupportedOperationException("TODO");
            } else if (type instanceof NullableType) {
                return create(((NullableType)type).getTargetType());
            } else if (type instanceof EnumType) {
                Map<String, Object> map = new LinkedHashMap<>(3);
                map.put("type", "string");
                map.put("enum", ((EnumType)type).getConstants().stream().map(EnumType.Constant::getName).collect(Collectors.toList()));
                return Collections.unmodifiableMap(map);
            } else  {
                SimpleType simpleType = (SimpleType) type;
                Class<?> javaType = simpleType.getJavaType();
                if (boolean.class == javaType) {
                    return Collections.singletonMap("type", "boolean");
                } else if (char.class == javaType) {
                    return Collections.singletonMap("type", "string");
                } else if (byte.class == javaType) {
                    return Collections.singletonMap("type", "integer");
                } else if (short.class == javaType) {
                    return Collections.singletonMap("type", "integer");
                } else if (int.class == javaType) {
                    Map<String, Object> map = new LinkedHashMap<>(3);
                    map.put("type", "integer");
                    map.put("format", "int32");
                    return Collections.unmodifiableMap(map);
                } else if (long.class == javaType) {
                    Map<String, Object> map = new LinkedHashMap<>(3);
                    map.put("type", "integer");
                    map.put("format", "int32");
                    return Collections.unmodifiableMap(map);
                } else if (float.class == javaType) {
                    Map<String, Object> map = new LinkedHashMap<>(3);
                    map.put("type", "number");
                    map.put("format", "float");
                    return Collections.unmodifiableMap(map);
                } else if (double.class == javaType) {
                    Map<String, Object> map = new LinkedHashMap<>(3);
                    map.put("type", "number");
                    map.put("format", "double");
                    return Collections.unmodifiableMap(map);
                }
                return Collections.singletonMap("type", "string");
            }
        }
    }

    private static class Response {

        final Type type;

        final String doc;

        private Response(Type type, String doc) {
            this.type = type;
            this.doc = doc;
        }
    }
}
