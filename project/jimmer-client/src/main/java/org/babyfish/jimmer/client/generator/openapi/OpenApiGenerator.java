package org.babyfish.jimmer.client.generator.openapi;

import org.babyfish.jimmer.client.generator.Namespace;
import org.babyfish.jimmer.client.runtime.*;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class OpenApiGenerator {

    private final Metadata metadata;

    private final Map<String, Object> headers;

    private final ServiceNameManager serviceNameManager = new ServiceNameManager();

    private final OperationNameManager operationNameManager = new OperationNameManager();

    private final FetcherContext fetcherContext = new FetcherContext();

    private final TypeNameManager typeNameManager = new TypeNameManager(fetcherContext);

    public OpenApiGenerator(Metadata metadata,Map<String, Object> headers) {
        if (!metadata.isGenericSupported()) {
            throw new IllegalArgumentException("OpenApiGenerator does not support generic");
        }
        this.metadata = metadata;
        this.headers = headers;
        for (ObjectType fetchedType : metadata.getFetchedTypes()) {
            fetcherContext.fetch(fetchedType.getFetchByInfo(), () -> {
                allocateFetchedTypeNames(fetchedType);
            });
        }
    }

    private void allocateFetchedTypeNames(ObjectType objectType) {
        typeNameManager.get(objectType);
        for (Property property : objectType.getProperties().values()) {
            Type type = property.getType();
            if (type instanceof NullableType) {
                type = ((NullableType)type).getTargetType();
            }
            if (type instanceof ListType) {
                type = ((ListType)type).getElementType();
            }
            if (type instanceof ObjectType) {
                ObjectType targetType = (ObjectType) type;
                if (targetType.getKind() == ObjectType.Kind.FETCHED) {
                    typeNameManager.get(targetType);
                }
            }
        }
    }

    public void generate(Writer writer) throws IOException {
        YmlWriter ymlWriter = new YmlWriter(writer);
        generatePaths(ymlWriter);
    }

    private void generatePaths(YmlWriter writer) {
        writer.object("paths", ()-> {
            for (Service service : metadata.getServices()) {
                for (Operation operation : service.getOperations()) {
                    writer.object(operation.getUri(), () -> {
                        writer.list("tags", () -> {
                            writer.code(serviceNameManager.get(service));
                        });
                        writer.prop("operationId", operationNameManager.get(operation));
                        if (!operation.getParameters().isEmpty()) {
                            writer.list("parameters", () -> {
                                for (Parameter parameter : operation.getParameters()) {
                                    if (parameter.isRequestBody()) {
                                        writer.object("requestBody", () -> {
                                            writer.object("content", () -> {
                                                writer.object("application/json", () -> {
                                                    writer.object("schema", () -> {
                                                        generateType(parameter.getType(), writer);
                                                    });
                                                });
                                            });
                                        });
                                        continue;
                                    }
                                    String requestParam = parameter.getRequestParam();
                                    String name = requestParam != null ? requestParam : parameter.getPathVariable();
                                    if (name != null) {
                                        writer.listItem(() -> {
                                            writer.prop("name", name);
                                            writer.prop("in", requestParam != null ? "query" : "path");
                                            if (!(parameter.getType() instanceof NullableType)) {
                                                writer.prop("required", "true");
                                            }
                                            writer.object("schema", () -> {
                                                this.generateType(parameter.getType(), writer);
                                            });
                                        });
                                    } else {
                                        for (Property property : ((ObjectType)parameter.getType()).getProperties().values()) {
                                            writer.listItem(() -> {
                                                writer.prop("name", property.getName());
                                                writer.prop("in", "query");
                                                if (!(property.getType() instanceof NullableType)) {
                                                    writer.prop("required", "true");
                                                }
                                                writer.object("schema", () -> {
                                                    this.generateType(property.getType(), writer);
                                                });
                                            });
                                        }
                                    }
                                }
                            });
                        }
                    });
                    generateResponses(operation, writer);
                }
            }
        });
    }

    private void generateType(Type type, YmlWriter writer) {
        if (type instanceof ObjectType) {
            writer.prop("$ref", "#/components/schemas/" + typeNameManager.get((ObjectType) type));
        } else if (type instanceof ListType) {
            writer
                    .prop("type", "array")
                    .object("items", () -> {
                        generateType(((ListType)type).getElementType(), writer);
                    });
        } else if (type instanceof MapType) {
            throw new UnsupportedOperationException("TODO");
        } else if (type instanceof NullableType) {
            generateType(((NullableType)type).getTargetType(), writer);
        } else if (type instanceof EnumType) {
            writer.prop("type", "string");
            writer.list("enum", () -> {
                for (EnumType.Constant constant : ((EnumType)type).getConstants()) {
                    writer.listItem(() -> writer.code(constant.getName()).code('\n'));
                }
            });
        } else  {
            SimpleType simpleType = (SimpleType) type;
            Class<?> javaType = simpleType.getJavaType();
            if (boolean.class == javaType) {
                writer.prop("type", "boolean");
            } else if (char.class == javaType) {
                writer.prop("type", "string");
            } else if (byte.class == javaType) {
                writer.prop("type", "integer");
            } else if (short.class == javaType) {
                writer.prop("type", "integer");
            } else if (int.class == javaType) {
                writer.prop("type", "integer");
                writer.prop("format", "int32");
            } else if (long.class == javaType) {
                writer.prop("type", "integer");
                writer.prop("format", "int64");
            } else if (float.class == javaType) {
                writer.prop("type", "number");
                writer.prop("format", "float");
            } else if (double.class == javaType) {
                writer.prop("type", "number");
                writer.prop("format", "double");
            } else if (BigDecimal.class == javaType) {
                writer.prop("type", "number");
            } else if (BigInteger.class == javaType) {
                writer.prop("type", "number");
            } else {
                writer.prop("type", "string");
            }
        }
    }

    private void generateResponses(Operation operation, YmlWriter writer) {
        Map<Integer, List<ObjectType>> exceptionTypeMap = new TreeMap<>();
        for (ObjectType exceptionType : operation.getExceptionTypes()) {
            exceptionTypeMap.computeIfAbsent(500, it -> new ArrayList<>())
                    .add(exceptionType);
        }
        writer.object("responses", () -> {
            writer.object("200", () -> {
                String returnDoc = operation.getDoc() != null ? operation.getDoc().getReturnValue() : null;
                writer.prop("description", returnDoc != null ? returnDoc : "OK");
                if (operation.getReturnType() != null) {
                    writer.object("schema", () -> {
                        generateType(operation.getReturnType(), writer);
                    });
                }
            });
            for (Map.Entry<Integer, List<ObjectType>> e : exceptionTypeMap.entrySet()) {
                writer.object(e.getKey().toString(), () -> {
                    List<ObjectType> exceptionTypes = e.getValue();
                    writer.prop("description", "ERROR");
                    writer.object("schema", () -> {
                        if (exceptionTypes.size() == 1) {
                            generateType(operation.getReturnType(), writer);
                        } else {
                            writer.list("oneOf", () -> {
                                for (ObjectType exceptionType : exceptionTypes) {
                                    writer.listItem(() -> {
                                        generateType(exceptionType, writer);
                                    });
                                }
                            });
                        }
                    });
                });
            }
        });
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
            return fetchByInfoStack.peek();
        }


    }

    private static class TypeNameManager {

        private final FetcherContext ctx;

        private final Map<Type, String> typeNameMap = new HashMap<>();

        private final Namespace namespace = new Namespace();

        private TypeNameManager(FetcherContext ctx) {
            this.ctx = ctx;
        }

        public String get(ObjectType type) {
            String typeName = typeNameMap.get(type);
            if (typeName == null) {
                typeName = getImpl(type);
                typeNameMap.put(type, typeName);
            }
            return typeName;
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
}
