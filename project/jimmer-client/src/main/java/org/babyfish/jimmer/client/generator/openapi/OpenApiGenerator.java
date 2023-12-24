package org.babyfish.jimmer.client.generator.openapi;

import org.babyfish.jimmer.client.generator.Namespace;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.runtime.*;

import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class OpenApiGenerator {

    private final Metadata metadata;

    private final Map<String, Object> headers;

    private final ServiceNameManager serviceNameManager = new ServiceNameManager();

    private final OperationNameManager operationNameManager = new OperationNameManager();

    private final TypeNameManager typeNameManager;

    public OpenApiGenerator(Metadata metadata,Map<String, Object> headers) {
        if (metadata.isGenericSupported()) {
            throw new IllegalArgumentException("OpenApiGenerator does not support generic");
        }
        this.metadata = metadata;
        this.headers = headers;
        this.typeNameManager = new TypeNameManager(metadata);
    }

    public void generate(Writer writer) {
        YmlWriter ymlWriter = new YmlWriter(writer);
        ymlWriter.prop("openapi", "3.0.1");
        generatePaths(ymlWriter);
        generateTypeDefinitions(ymlWriter);
    }

    private void generatePaths(YmlWriter writer) {
        writer.object("paths", ()-> {
            for (Map.Entry<String, List<Operation>> e : metadata.getPathMap().entrySet()) {
                writer.object(e.getKey(), () -> {
                    for (Operation operation : e.getValue()) {
                        for (Operation.HttpMethod method : operation.getHttpMethods()) {
                            writer.object(method.name().toLowerCase(), () -> {
                                generateOperation(operation, writer);
                            });
                        }
                    }
                });
            }
        });
    }

    private void generateOperation(Operation operation, YmlWriter writer) {
        writer.description(Description.of(Doc.valueOf(operation.getDoc()), true));
        writer.list("tags", () -> {
            writer.listItem(() -> {
                writer.code(serviceNameManager.get(operation.getDeclaringService()));
            });
        });
        writer.prop("operationId", operationNameManager.get(operation));
        List<Parameter> httpParameters = operation.getParameters().stream().filter(it -> !it.isRequestBody()).collect(Collectors.toList());
        Parameter requestBodyParameter = operation.getParameters().stream().filter(it -> it.isRequestBody()).findFirst().orElse(null);
        if (!httpParameters.isEmpty()) {
            writer.list("parameters", () -> {
                for (Parameter parameter : httpParameters) {
                    String requestHeader = parameter.getRequestHeader();
                    String requestParam = parameter.getRequestParam();
                    String name = requestHeader != null ?
                            requestHeader :
                            requestParam != null ? requestParam : parameter.getPathVariable();
                    if (name != null) {
                        writer.listItem(() -> {
                            writer.prop("name", name);
                            writer.prop(
                                    "in",
                                    requestHeader != null ?
                                            "header" :
                                            requestParam != null ? "query" : "path"
                            );
                            if (!(parameter.getType() instanceof NullableType)) {
                                writer.prop("required", "true");
                            }
                            writer.description(
                                    Description.of(Doc.paramOf(operation.getDoc(), parameter.getName()))
                            );
                            writer.object("schema", () -> {
                                this.generateType(parameter.getType(), writer);
                                if (parameter.getDefaultValue() != null) {
                                    writer.prop("default", parameter.getDefaultValue());
                                }
                            });
                        });
                    } else {
                        for (Property property : ((ObjectType) parameter.getType()).getProperties().values()) {
                            writer.listItem(() -> {
                                writer.prop("name", property.getName());
                                writer.prop("in", "query");
                                if (!(property.getType() instanceof NullableType)) {
                                    writer.prop("required", "true");
                                }
                                String doc = Doc.valueOf(property.getDoc());
                                if (doc == null) {
                                    doc = Doc.propertyOf(((ObjectType) parameter.getType()).getDoc(), property.getName());
                                }
                                writer.description(Description.of(doc));
                                writer.object("schema", () -> {
                                    this.generateType(property.getType(), writer);
                                });
                            });
                        }
                    }
                }
            });
            if (requestBodyParameter != null) {
                writer.object("requestBody", () -> {
                    writer.object("content", () -> {
                        writer.object("application/json", () -> {
                            writer.object("schema", () -> {
                                generateType(requestBodyParameter.getType(), writer);
                            });
                        });
                    });
                    if (!(requestBodyParameter.getType() instanceof NullableType)) {
                        writer.prop("required", "true");
                    }
                    writer.description(
                            Description.of(Doc.paramOf(operation.getDoc(), requestBodyParameter.getName()))
                    );
                });
            }
        }
        generateResponses(operation, writer);
    }

    private void generateType(Type type, YmlWriter writer) {
        if (type instanceof ObjectType) {
            writer.prop("$ref", "'#/components/schemas/" + typeNameManager.get((ObjectType) type) + '\'');
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
                writer.description(Description.of(returnDoc != null ? returnDoc : "OK"));
                if (operation.getReturnType() != null) {
                    writer.object("content", () -> {
                        writer.object("application/json", () -> {
                            writer.object("schema", () -> {
                                generateType(operation.getReturnType(), writer);
                            });
                        });
                    });
                }
            });
            for (Map.Entry<Integer, List<ObjectType>> e : exceptionTypeMap.entrySet()) {
                writer.object(e.getKey().toString(), () -> {
                    List<ObjectType> exceptionTypes = e.getValue();
                    writer.prop("description", "ERROR");
                    writer.object("content", () -> {
                        writer.object("application/json", () -> {
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
                    });
                });
            }
        });
    }

    private void generateTypeDefinitions(YmlWriter writer) {
        writer.object("components", () -> {
           writer.object("schemas", () -> {
               for (ObjectType fetchedType : typeNameManager.exportObjectTypes().values()) {
                   generateTypeDefinition(fetchedType, writer);
               }
           });
        });
    }

    private void generateTypeDefinition(ObjectType type, YmlWriter writer) {
        writer.object(typeNameManager.get(type), () -> {
            writer.prop("type", "object");
            writer.description(Description.of(Doc.valueOf(type.getDoc())));
            writer.object("properties", () -> {
                for (Property property : type.getProperties().values()) {
                    writer.object(property.getName(), () -> {
                        String doc = Doc.valueOf(property.getDoc());
                        if (doc == null) {
                            doc = Doc.propertyOf(type.getDoc(), property.getName());
                        }
                        writer.description(Description.of(doc));
                        generateType(property.getType(), writer);
                    });
                }
            });
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

    private static class TypeNameManager {

        private final Map<Type, String> typeNameMap = new HashMap<>();

        private final Namespace namespace = new Namespace();

        public TypeNameManager(Metadata metadata) {
            for (ObjectType fetchedType : metadata.getFetchedTypes()) {
                get(fetchedType);
            }
            for (ObjectType dynamicType : metadata.getDynamicTypes()) {
                get(dynamicType);
            }
            for (ObjectType staticType : metadata.getStaticTypes()) {
                get(staticType);
            }
        }

        public String get(ObjectType type) {
            String typeName = typeNameMap.get(type);
            if (typeName == null) {
                typeName = namespace.allocate(getImpl(type));
                typeNameMap.put(type, typeName);
            }
            return typeName;
        }

        private String getImpl(Type type) {
            if (type instanceof ObjectType) {
                ObjectType objectType = (ObjectType) type;
                StringBuilder builder = new StringBuilder();
                if (objectType.getKind() == ObjectType.Kind.DYNAMIC) {
                    builder.append("Dynamic_");
                }
                builder.append(String.join("_", objectType.getSimpleNames()));
                FetchByInfo info = null;
                if (objectType.getKind() == ObjectType.Kind.FETCHED) {
                    info = objectType.getFetchByInfo();
                }
                if (info != null) {
                    builder.append('_').append(info.getOwnerType().getSimpleName())
                            .append('_').append(info.getConstant());
                }
                for (Type argument : objectType.getArguments()) {
                    builder.append('_').append(getImpl(argument));
                }
                String name = builder.toString();
                if (info != null) {
                    collectMoreFetchedTypeNames(objectType, name);
                }
                return name;
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

        private void collectMoreFetchedTypeNames(ObjectType objectType, String prefix) {
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
                        if (!typeNameMap.containsKey(targetType)) {
                            String name = prefix + '_' + property.getName();
                            typeNameMap.put(targetType, name);
                            collectMoreFetchedTypeNames(targetType, name);
                        }
                    }
                }
            }
        }

        public NavigableMap<String, ObjectType> exportObjectTypes() {
            NavigableMap<String, ObjectType> typeMap = new TreeMap<>();
            for (Map.Entry<Type, String> e : typeNameMap.entrySet()) {
                Type type = e.getKey();
                if (type instanceof ObjectType) {
                    typeMap.put(e.getValue(), (ObjectType) type);
                }
            }
            return typeMap;
        }
    }
}
