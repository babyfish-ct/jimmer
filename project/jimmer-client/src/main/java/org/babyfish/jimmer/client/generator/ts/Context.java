package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.meta.*;

import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Function;

public class Context {

    private final OutputStream out;

    private final String indent;

    private final Map<Service, File> serviceFileMap;

    private final Map<Operation, String> operationNameMap;

    private final Map<Type, File> typeFileMap;

    public Context(Metadata metadata, OutputStream out, String indent) {
        this.out = out;
        this.indent = indent;
        VisitorImpl impl = new VisitorImpl();
        for (Service service : metadata.getServices().values()) {
            service.accept(impl);
        }
        serviceFileMap = Collections.unmodifiableMap(impl.serviceNamespace.getFileMap());
        operationNameMap = Collections.unmodifiableMap(impl.operationNameMap);
        typeFileMap = Collections.unmodifiableMap(impl.typeNamespace.getFileMap());
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public String getIndent() {
        return indent;
    }

    public File file(Service service) {
        return serviceFileMap.get(service);
    }

    public File file(Type type) {
        return typeFileMap.get(type);
    }

    public Map<Service, File> getServiceFileMap() {
        return serviceFileMap;
    }

    public String operationName(Operation operation) {
        return operationNameMap.get(operation);
    }

    public Map<Type, File> getTypeFileMap() {
        return typeFileMap;
    }

    private static class VisitorImpl implements Visitor {

        private String serviceName;

        private String operationName;

        final Map<Operation, String> operationNameMap = new HashMap<>();

        final Namespace<Service> serviceNamespace = new Namespace<>(
                service -> new File("service", service.getRawType().getSimpleName())
        );

        private final Namespace<Operation> operationNamespace = new Namespace<>(
                operation -> new File("<fake>", serviceName + "::" + operation.getName())
        );

        final Namespace<Type> typeNamespace = new Namespace<>(
                type -> {
                    if (type instanceof ImmutableObjectType) {
                        ImmutableObjectType immutableObjectType = (ImmutableObjectType) type;
                        switch (immutableObjectType.getCategory()) {
                            case FETCH:
                                return new File(
                                        "model/immutable/fetch",
                                        serviceName + '_' +
                                                operationName + '_' +
                                                immutableObjectType.getJavaType().getSimpleName()
                                );
                            case VIEW:
                                return new File(
                                        "model/immutable/view",
                                        immutableObjectType.getJavaType().getSimpleName() + "_View"
                                );
                            case RAW:
                                return new File(
                                        "model/immutable",
                                        immutableObjectType.getJavaType().getSimpleName()
                                );
                        }
                    } else if (type instanceof StaticObjectType) {
                        Class<?> javaType = ((StaticObjectType)type).getJavaType();
                        return new File("model/static", javaType.getSimpleName());
                    } else if (type instanceof EnumType) {
                        return new File(
                                "model/enum",
                                ((EnumType)type).getJavaType().getSimpleName()
                        );
                    }
                    return null;
                }
        );

        @Override
        public void visitingService(Service service) {
            serviceName = serviceNamespace.add(service).getName();
        }

        @Override
        public void visitedService(Service service) {
            serviceName = null;
        }

        @Override
        public void visitingOperation(Operation operation) {
            String name = operationNamespace.add(operation).getName();
            name = name.substring(name.indexOf("::") + 2);
            operationNameMap.put(operation, name);
            operationName = name;
        }

        @Override
        public void visitedOperation(Operation operation) {
            operationName = null;
        }

        @Override
        public boolean isTypeVisitable(Type type) {
            return !typeNamespace.getFileMap().containsKey(type);
        }

        @Override
        public void visitImmutableObjectType(ImmutableObjectType immutableObjectType) {
            typeNamespace.add(immutableObjectType);
        }

        @Override
        public void visitStaticObjectType(StaticObjectType staticObjectType) {
            typeNamespace.add(staticObjectType);
        }

        @Override
        public void visitEnumType(EnumType enumType) {
            typeNamespace.add(enumType);
        }
    }

    private static class Namespace<N extends Node> {

        private final Map<N, File> fileMap = new IdentityHashMap<>();

        private final Map<String, Integer> nameCountMap = new HashMap<>();

        private final Function<N, File> fileCreator;

        private Namespace(Function<N, File> fileCreator) {
            this.fileCreator = fileCreator;
        }

        public File add(N node) {
            File file = fileMap.get(node);
            if (file == null) {
                file = fileCreator.apply(node);
                if (file == null) {
                    return null;
                }
                String name = file.getName();
                Integer count = nameCountMap.get(name);
                if (count == null) {
                    nameCountMap.put(name, 1);
                } else {
                    count++;
                    nameCountMap.put(name, count);
                    file = new File(file.getDir(), name + '_' + count);
                }
                fileMap.put(node, file);
            }
            return file;
        }

        public Map<N, File> getFileMap() {
            return fileMap;
        }
    }
}
