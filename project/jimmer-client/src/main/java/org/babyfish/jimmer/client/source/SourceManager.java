package org.babyfish.jimmer.client.source;

import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.runtime.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public abstract class SourceManager {

    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("/|\\.");

    private final Context ctx;

    private final IdentityHashMap<Service, Source> serviceSourceMap = new IdentityHashMap<>();

    private final IdentityHashMap<Operation, Source> operationSourceMap = new IdentityHashMap<>();

    private final IdentityHashMap<Type, Source> typeSourceMap = new IdentityHashMap<>();

    private final Map<String, Source> rootSourceMap = new HashMap<>();

    protected SourceManager(Context ctx) {
        this.ctx = ctx;
    }

    public Collection<Source> getRootSources() {
        return Collections.unmodifiableCollection(rootSourceMap.values());
    }

    public Source getRootSource(String name) {
        Source source = rootSourceMap.get(name);
        if (source == null) {
            throw new IllegalStateException("No source \"" + name + "\"");
        }
        return source;
    }

    public final Source getSource(Service service) {
        Source source = serviceSourceMap.get(service);
        if (source != null) {
            return source;
        }
        source = createServiceSource(service);
        if (source == null) {
            throw new IllegalStateException(
                    "The \"createServiceSource\" of \"" + getClass().getName() + "\" cannot return null"
            );
        }
        serviceSourceMap.put(service, source);
        return source;
    }

    public final Source getSource(Operation operation) {
        Source source = operationSourceMap.get(operation);
        if (source != null) {
            return source;
        }
        source = createOperationSource(operation);
        if (source == null) {
            throw new IllegalStateException(
                    "The \"createOperationSource\" of \"" + getClass().getName() + "\" cannot return null"
            );
        }
        operationSourceMap.put(operation, source);
        return source;
    }

    public final Source getSource(Type type) {
        if (type instanceof ObjectType) {
            ObjectType objectType = (ObjectType) type;
            ObjectType unwrapped = objectType.unwrap();
            return objectTypeSource(unwrapped != null ? unwrapped : objectType);
        } else if (type instanceof EnumType) {
            return enumTypeSource((EnumType) type);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected final <C extends Context> C getContext() {
        return (C) ctx;
    }

    protected final Source createRootSource(String dir, String name, Supplier<Render> renderSupplier) {
        List<String> dirs = dirs(dir);
        String key = AbstractSource.toString(dirs, name);
        if (rootSourceMap.containsKey(key)) {
            throw new IllegalStateException("The source \"" + key + "\" already exists");
        }
        Source source = new SourceFile(dirs, name, renderSupplier.get());
        rootSourceMap.put(key, source);
        return source;
    }

    private Source objectTypeSource(ObjectType objectType) {
        Source source = typeSourceMap.get(objectType);
        if (source != null) {
            return source;
        }
        if (objectType.getKind() == ObjectType.Kind.ERROR) {
            source = createDynamicTypeSource(objectType);
            if (source == null) {
                throw new IllegalStateException(
                        "The \"createErrorTypeSource\" of \"" + getClass().getName() + "\" cannot return null"
                );
            }
        } else if (objectType.getKind() == ObjectType.Kind.STATIC) {
            ObjectType unwrapped = objectType.unwrap();
            source = createStaticTypeSource(unwrapped != null ? unwrapped : objectType);
            if (source == null) {
                throw new IllegalStateException(
                        "The \"createStaticTypeSource\" of \"" + getClass().getName() + "\" cannot return null"
                );
            }
        } else if (objectType.getKind() == ObjectType.Kind.DYNAMIC) {
            source = createDynamicTypeSource(objectType);
            if (source == null) {
                throw new IllegalStateException(
                        "The \"createDynamicTypeSource\" of \"" + getClass().getName() + "\" cannot return null"
                );
            }
        } else {
            source = createFetchedTypeSource(objectType);
            if (source == null) {
                throw new IllegalStateException(
                        "The \"createFetchedTypeSource\" of \"" + getClass().getName() + "\" cannot return null"
                );
            }
        }
        typeSourceMap.put(objectType, source);
        return source;
    }

    private Source enumTypeSource(EnumType enumType) {
        Source source = typeSourceMap.get(enumType);
        if (source != null) {
            return source;
        }
        source = createEnumTypeSource(enumType);
        if (source == null) {
            throw new IllegalStateException(
                    "The \"createEnumTypeSource\" of \"" + getClass().getName() + "\" cannot return null"
            );
        }
        typeSourceMap.put(enumType, source);
        return source;
    }

    protected abstract Source createServiceSource(Service service);

    protected abstract Source createOperationSource(Operation operation);

    protected abstract Source createStaticTypeSource(ObjectType objectType);

    protected abstract Source createFetchedTypeSource(ObjectType objectType);

    protected abstract Source createDynamicTypeSource(ObjectType objectType);

    protected abstract Source createErrorTypeSource(ObjectType objectType);

    protected abstract Source createEnumTypeSource(EnumType enumType);

    public void createAdditionalSources() {}

    private static List<String> dirs(String dir) {
        if (dir.startsWith("/")) {
            dir = dir.substring(1);
        }
        if (dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }
        if (dir.isEmpty()) {
            return Collections.emptyList();
        }
        String[] arr = SEPARATOR_PATTERN.split(dir);
        List<String> list = new ArrayList<>(arr.length);
        list.addAll(Arrays.asList(arr));
        return Collections.unmodifiableList(list);
    }
}
