package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.client.meta.impl.ImmutableObjectTypeImpl;

import java.io.OutputStream;
import java.util.*;
import java.util.function.Function;

public class Context {

    private static final Comparator<Service> SERVICE_COMPARATOR =
            Comparator.comparing(a -> a.getJavaType().getName());

    private static final Comparator<ImmutableObjectType> DTO_COMPARATOR =
            Comparator
                    .comparing(Context::totalPropCount)
                    .thenComparing((ImmutableObjectType it) -> it.getJavaType().getName())
                    .thenComparing((ImmutableObjectType it) -> {
                        FetchByInfo info = it.getFetchByInfo();
                        return info != null ? info.getOwnerType().getName() : "";
                    })
                    .thenComparing((ImmutableObjectType it) -> {
                        FetchByInfo info = it.getFetchByInfo();
                        return info != null ? info.getConstant() : "";
                    });

    private final OutputStream out;

    private final File moduleFile;

    private final String indent;

    private final Map<Class<?>, List<ImmutableObjectType>> dtoMap;

    private final Map<Class<?>, StaticObjectType> genericTypeMap;

    private final Map<Operation, String> operationNameMap;

    private final Namespace<Type> typeNamespace;

    private final NavigableMap<Service, File> serviceFileMap;

    private final Map<Type, File> typeFileMap;

    private final Namespace<Class<?>> fetchByOwnerNamespace;

    private final Namespace<Class<?>> dtoPrefixNamespace;

    public Context(Metadata metadata, OutputStream out, String moduleName, int indent) {
        this.out = out;
        this.moduleFile = new File("", moduleName);
        if (indent < 2) {
            throw new IllegalArgumentException("indent cannot be less than 2");
        }
        StringBuilder builder = new StringBuilder();
        for (int i = indent; i > 0; --i) {
            builder.append(' ');
        }
        this.indent = builder.toString();
        this.genericTypeMap = metadata.getGenericTypes();

        VisitorImpl impl = new VisitorImpl();
        for (Service service : metadata.getServices().values()) {
            service.accept(impl);
        }
        for (StaticObjectType genericType : metadata.getGenericTypes().values()) {
            genericType.accept(impl);
        }

        operationNameMap = impl.operationNamespace.getNameMap();

        Map<Class<?>, List<ImmutableObjectType>> dtoMap = new HashMap<>();
        for (ImmutableObjectType immutableObjectType : metadata.getFetchedImmutableObjectTypes().values()) {
            dtoMap
                    .computeIfAbsent(immutableObjectType.getJavaType(), it -> new ArrayList<>())
                    .add(immutableObjectType);
        }
        for (ImmutableObjectType immutableObjectType : metadata.getViewImmutableObjectTypes().values()) {
            dtoMap
                    .computeIfAbsent(immutableObjectType.getJavaType(), it -> new ArrayList<>())
                    .add(immutableObjectType);
        }
        for (Map.Entry<Class<?>, List<ImmutableObjectType>> e : dtoMap.entrySet()) {
            List<ImmutableObjectType> types = e.getValue();
            types.sort(DTO_COMPARATOR);
            e.setValue(Collections.unmodifiableList(types));
        }
        this.dtoMap = Collections.unmodifiableMap(dtoMap);

        typeNamespace = impl.typeNamespace;

        NavigableMap<Service, File> map = new TreeMap<>(SERVICE_COMPARATOR);
        map.putAll(impl.serviceFileManager.getFileMap());
        serviceFileMap = Collections.unmodifiableNavigableMap(map);

        typeFileMap = impl.typeFileManager.getFileMap();

        fetchByOwnerNamespace = impl.fetchByOwnerNamespace;

        dtoPrefixNamespace = new Namespace<>(clazz -> clazz.getSimpleName() + "Dto");
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public File getModuleFile() {
        return moduleFile;
    }

    public String getIndent() {
        return indent;
    }

    public File getFile(Service service) {
        if (service == null) {
            throw new IllegalArgumentException("service cannot be null");
        }
        return serviceFileMap.get(service);
    }

    public File getFile(Type type) {
        return typeFileMap.get(rawType(type));
    }

    public String getOperationName(Operation operation) {
        return operationNameMap.get(operation);
    }

    public Map<Service, File> getServiceFileMap() {
        return serviceFileMap;
    }

    public Iterable<Map.Entry<Type, File>> getTypeFilePairs() {
        return () -> typeFileMap.entrySet().iterator();
    }

    public Map<Class<?>, List<ImmutableObjectType>> getDtoMap() {
        return dtoMap;
    }

    public String getDtoPrefix(Class<?> rawType) {
        return dtoPrefixNamespace.get(rawType);
    }

    public String getDtoSuffix(ImmutableObjectType type) {
        FetchByInfo fetchByInfo = type.getFetchByInfo();
        if (fetchByInfo != null) {
            return fetchByOwnerNamespace.get(fetchByInfo.getOwnerType()) + "/" + fetchByInfo.getConstant();
        }
        if (type.getCategory() == ImmutableObjectType.Category.VIEW) {
            return "DEFAULT";
        }
        return null;
    }

    private Type rawType(Type type) {
        if (type instanceof StaticObjectType) {
            StaticObjectType staticObjectType = (StaticObjectType) type;
            if (!staticObjectType.getTypeArguments().isEmpty()) {
                return genericTypeMap.get(staticObjectType.getJavaType());
            }
        }
        return type;
    }

    private static int totalPropCount(ImmutableObjectType type) {
        int count = type.getProperties().size();
        for (Property prop : type.getProperties().values()) {
            if (prop.getType() instanceof ImmutableObjectType) {
                count += totalPropCount((ImmutableObjectType) prop.getType());
            }
        }
        return count;
    }

    private static class VisitorImpl implements Visitor {

        private String serviceName;

        private String operationName;

        final Namespace<Service> serviceNamespace = new Namespace<>(
                service -> service.getJavaType().getSimpleName()
        );

        final Namespace<Operation> operationNamespace = new Namespace<>(
                operation -> serviceName + "::" + operation.getName(),
                name -> name.substring(name.indexOf("::") + 2)
        );

        final Namespace<Type> typeNamespace = new Namespace<>(
                type -> {
                    if (type instanceof SimpleType) {
                        return CodeWriter.SIMPLE_TYPE_NAMES.get(((SimpleType)type).getJavaType());
                    }
                    if (type instanceof ImmutableObjectType) {
                        return ((ImmutableObjectType)type).getJavaType().getSimpleName();
                    } else if (type instanceof StaticObjectType) {
                        Class<?> javaType = ((StaticObjectType)type).getJavaType();
                        return javaType.getSimpleName();
                    } else if (type instanceof EnumType) {
                        return ((EnumType)type).getJavaType().getSimpleName();
                    }
                    return null;
                }
        );

        final FileManager<Service> serviceFileManager = new FileManager<>(
                service -> "services",
                serviceNamespace
        );

        final FileManager<Type> typeFileManager = new FileManager<>(
                type -> {
                    if (!type.hasDefinition()) {
                        return null;
                    }
                    if (type instanceof ImmutableObjectType) {
                        return "model/entities";
                    } else if (type instanceof StaticObjectType) {
                        Class<?> javaType = ((StaticObjectType)type).getJavaType();
                        return "model/static";
                    } else if (type instanceof EnumType) {
                        return "model/enums";
                    }
                    return null;
                },
                typeNamespace
        );

        final Namespace<Class<?>> fetchByOwnerNamespace = new Namespace<>(Class::getSimpleName);

        private final Map<Class<?>, List<ImmutableObjectType>> dtoMap = new HashMap<>();

        @Override
        public void visitingService(Service service) {
            serviceName = serviceFileManager.add(service).getName();
            // Keep some fetcher owner names are same with service names
            fetchByOwnerNamespace.get(service.getClass());
        }

        @Override
        public void visitedService(Service service) {
            serviceName = null;
        }

        @Override
        public void visitingOperation(Operation operation) {
            operationName = operationNamespace.get(operation);
        }

        @Override
        public void visitedOperation(Operation operation) {
            operationName = null;
        }

        @Override
        public boolean isTypeVisitable(Type type) {
            return !typeFileManager.getFileMap().containsKey(type);
        }

        @Override
        public void visitImmutableObjectType(ImmutableObjectType immutableObjectType) {
            if (typeFileManager.add(immutableObjectType) == null) {
                dtoMap
                        .computeIfAbsent(immutableObjectType.getJavaType(), it -> new ArrayList<>())
                        .add(immutableObjectType);
            }
        }

        @Override
        public void visitStaticObjectType(StaticObjectType staticObjectType) {
            typeFileManager.add(staticObjectType);
        }

        @Override
        public void visitEnumType(EnumType enumType) {
            typeFileManager.add(enumType);
        }
    }

    private static class Namespace<T> {

        private final Map<T, String> nameMap = new IdentityHashMap<>();

        private final Map<String, Integer> nameCountMap = new HashMap<>();

        private final Function<T, String> initializer;

        private final Function<String, String> terminator;

        Namespace(Function<T, String> initializer) {
            this.initializer = initializer;
            this.terminator = null;
        }

        Namespace(Function<T, String> initializer, Function<String, String> terminator) {
            this.initializer = initializer;
            this.terminator = terminator;
        }

        public String get(T node) {
            String name = nameMap.get(node);
            if (name == null) {
                name = initializer.apply(node);
                Integer count = nameCountMap.get(name);
                if (count == null) {
                    nameCountMap.put(name, 1);
                } else {
                    count++;
                    nameCountMap.put(name, count);
                    name = name + '_' + count;
                }
                if (terminator != null) {
                    name = terminator.apply(name);
                }
                nameMap.put(node, name);
            }
            return name;
        }

        public Map<T, String> getNameMap() {
            return Collections.unmodifiableMap(nameMap);
        }
    }

    private static class FileManager<N extends Node> {

        private final Map<N, File> fileMap = new IdentityHashMap<>();

        private final Function<N, String> dirSupplier;

        private final Namespace<N> namespace;

        private FileManager(Function<N, String> dirSupplier, Namespace<N> namespace) {
            this.dirSupplier = dirSupplier;
            this.namespace = namespace;
        }

        public File add(N node) {
            File file = fileMap.get(node);
            if (file == null && !fileMap.containsKey(node)) {
                String dir = dirSupplier.apply(node);
                if (dir == null) {
                    return null;
                }
                String name = namespace.get(node);
                file = new File(dir, name);
                fileMap.put(node, file);
            }
            return file;
        }

        public Map<N, File> getFileMap() {
            return Collections.unmodifiableMap(fileMap);
        }
    }
}
