package org.babyfish.jimmer.client.source;

import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.generator.SourceAwareRender;
import org.babyfish.jimmer.client.runtime.EnumType;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Type;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public abstract class SourceManager {

    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("/|\\.");

    private final Metadata metadata;

    private final boolean isNewestTypeSupported;

    private final IdentityHashMap<Type, Source> sourceMap = new IdentityHashMap<>();

    private final Map<String, Source> rootSourceMap = new HashMap<>();

    protected SourceManager(Metadata metadata, boolean isNewestTypeSupported) {
        this.metadata = metadata;
        this.isNewestTypeSupported = isNewestTypeSupported;
    }

    public Collection<Source> getRootSources() {
        return Collections.unmodifiableCollection(rootSourceMap.values());
    }

    public final Source getSource(Type type) {
        if (type instanceof ObjectType) {
            return objectTypeSource((ObjectType) type);
        } else if (type instanceof EnumType) {
            return enumTypeSource((EnumType) type);
        } else {
            return null;
        }
    }

    protected final Metadata getMetadata() {
        return metadata;
    }

    protected final boolean isNewestTypeSupported() {
        return isNewestTypeSupported;
    }

    protected final Source createRootSource(String dir, String name, Supplier<Render> renderSupplier) {
        List<String> dirs = dirs(dir);
        String key = AbstractSource.toString(dirs, name);
        if (rootSourceMap.containsKey(key)) {
            throw new IllegalStateException("The source \"" + key + "\" already exists");
        }
        Source source = new SourceFile(dirs, name, renderSupplier.get());
        rootSourceMap.put(key, source);
        AbstractSource.bindRenderBackReference(source);
        return source;
    }

    private Source objectTypeSource(ObjectType objectType) {
        Source source = sourceMap.get(objectType);
        if (source != null) {
            return source;
        }
        if (objectType.getError() != null) {
            source = createDynamicTypeSource(objectType);
            if (source == null) {
                throw new IllegalStateException(
                        "The \"getErrorTypeSource\" of \"" + getClass().getName() + "\" cannot return null"
                );
            }
        } else if (objectType.getImmutableType() == null) {
            ObjectType unwrapped = objectType.unwrap();
            source = createStaticTypeSource(unwrapped != null ? unwrapped : objectType);
            if (source == null) {
                throw new IllegalStateException(
                        "The \"getStaticTypeSource\" of \"" + getClass().getName() + "\" cannot return null"
                );
            }
        } else if (objectType.getFetchByInfo() == null) {
            source = createDynamicTypeSource(objectType);
            if (source == null) {
                throw new IllegalStateException(
                        "The \"getDynamicTypeSource\" of \"" + getClass().getName() + "\" cannot return null"
                );
            }
        } else {
            source = createFetchedTypeSource(objectType);
            if (source == null) {
                throw new IllegalStateException(
                        "The \"getFetchedTypeSource\" of \"" + getClass().getName() + "\" cannot return null"
                );
            }
        }
        sourceMap.put(objectType, source);
        return source;
    }

    private Source enumTypeSource(EnumType enumType) {
        Source source = sourceMap.get(enumType);
        if (source != null) {
            return source;
        }
        source = createEnumTypeSource(enumType);
        if (source == null) {
            throw new IllegalStateException(
                    "The \"getEnumTypeSource\" of \"" + getClass().getName() + "\" cannot return null"
            );
        }
        sourceMap.put(enumType, source);
        return source;
    }

    protected abstract Source createStaticTypeSource(ObjectType objectType);

    protected abstract Source createFetchedTypeSource(ObjectType objectType);

    protected abstract Source createDynamicTypeSource(ObjectType objectType);

    protected abstract Source createErrorTypeSource(ObjectType objectType);

    protected abstract Source createEnumTypeSource(EnumType enumType);

    private static List<String> dirs(String dir) {
        if (dir.startsWith("/")) {
            dir = dir.substring(1);
        }
        if (dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }
        String[] arr = SEPARATOR_PATTERN.split(dir);
        List<String> list = new ArrayList<>(arr.length);
        list.addAll(Arrays.asList(arr));
        return Collections.unmodifiableList(list);
    }
}
