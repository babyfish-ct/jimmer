package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.Prop;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Property;
import org.babyfish.jimmer.client.runtime.Type;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class ImmutableObjectTypeImpl extends Graph implements ObjectType {

    private final ImmutableType immutableType;

    private Map<String, Property> properties;

    private String fetchBy;

    private Doc doc;

    private Class<?> fetchOwner;

    public ImmutableObjectTypeImpl(ImmutableType immutableType) {
        this.immutableType = immutableType;
    }

    void init(String fetchBy, TypeName fetchOwner, TypeContext ctx) {
        this.fetchBy = fetchBy;
        Fetcher<?> fetcher;
        if (fetchBy != null) {
            Class<?> ownerType = ctx.javaType(fetchOwner);
            this.fetchOwner = ownerType;
            fetcher = staticFetcher(fetchBy, ownerType);
            if (fetcher == null) {
                fetcher = kotlinFetcher(fetchBy, ownerType);
                if (fetcher == null) {
                    throw new IllegalApiException(
                            "Illegal annotation \"@" +
                                    FetchBy.class.getName() +
                                    "\", there is no static fetcher \"" +
                                    fetchBy +
                                    "\" declared in \"" +
                                    ownerType.getName() +
                                    "\""
                    );
                }
            }
        } else {
            fetcher = null;
        }
        initProperties(fetcher, null, ctx);
    }

    private void initProperties(Fetcher<?> fetcher, Prop parentRecursiveProp, TypeContext ctx) {
        TypeDefinition definition = ctx.definition(this.immutableType.getJavaClass());
        ImmutableProp idProp = immutableType.getIdProp();
        Prop idMetaProp = definition.getPropMap().get(idProp.getName());
        Map<String, Property> properties = new LinkedHashMap<>();
        properties.put(
                idProp.getName(),
                new PropertyImpl(
                        idProp.getName(),
                        ctx.parseType(definition.getPropMap().get(idMetaProp.getName()).getType()),
                        idMetaProp.getDoc()
                )
        );
        if (fetcher != null) {
            for (org.babyfish.jimmer.sql.fetcher.Field field : fetcher.getFieldMap().values()) {
                ImmutableProp prop = field.getProp();
                Prop metaProp = definition.getPropMap().get(field.getProp().getName());
                Type type;
                if (prop.isAssociation(TargetLevel.ENTITY)) {
                    ImmutableObjectTypeImpl targetType = new ImmutableObjectTypeImpl(prop.getTargetType());
                    targetType.initProperties(field.getChildFetcher(), field.getRecursionStrategy() != null ? metaProp : null, ctx);
                    if (prop.isReferenceList(TargetLevel.ENTITY)) {
                        type = new ListTypeImpl(targetType);
                    } else {
                        type = targetType;
                        if (metaProp.getType().isNullable()) {
                            type = NullableTypeImpl.of(type);
                        }
                    }
                } else {
                    type = ctx.parseType(metaProp.getType());
                }
                properties.put(
                        prop.getName(),
                        new PropertyImpl(prop.getName(), type, metaProp.getDoc())
                );
            }
        } else {
            for (ImmutableProp prop : immutableType.getProps().values()) {
                Prop metaProp = definition.getPropMap().get(prop.getName());
                Type type;
                if (prop.isAssociation(TargetLevel.ENTITY)) {
                    ImmutableObjectTypeImpl targetType = new ImmutableObjectTypeImpl(prop.getTargetType());
                    targetType.initProperties(null, null, ctx);
                    if (prop.isReferenceList(TargetLevel.ENTITY)) {
                        type = new ListTypeImpl(targetType);
                    } else {
                        type = targetType;
                        if (metaProp.getType().isNullable()) {
                            type = NullableTypeImpl.of(type);
                        }
                    }
                } else {
                    type = ctx.parseType(metaProp.getType());
                }
                properties.put(
                        prop.getName(),
                        new PropertyImpl(prop.getName(), type, metaProp.getDoc())
                );
            }
        }
        if (parentRecursiveProp != null) {
            properties.put(
                    parentRecursiveProp.getName(),
                    new PropertyImpl(
                            parentRecursiveProp.getName(),
                            this,
                            parentRecursiveProp.getDoc()
                    )
            );
        }
        this.properties = Collections.unmodifiableMap(properties);
    }

    @Override
    public Class<?> getJavaType() {
        return immutableType.getJavaClass();
    }

    @Nullable
    @Override
    public ImmutableType getImmutableType() {
        return immutableType;
    }

    @Nullable
    @Override
    public String getFetchBy() {
        return fetchBy;
    }

    @Nullable
    @Override
    public Class<?> getFetchOwner() {
        return fetchOwner;
    }

    @Nullable
    @Override
    public Doc getDoc() {
        return doc;
    }

    void setDoc(Doc doc) {
        this.doc = doc;
    }

    @Nullable
    @Override
    public TypeDefinition.Error getError() {
        return null;
    }

    @Nullable
    @Override
    public List<Type> getArguments() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Property> getProperties() {
        return properties;
    }

    private Fetcher<?> staticFetcher(String fetchBy, Class<?> ownerType) {
        Field field;
        try {
            field = ownerType.getDeclaredField(fetchBy);
        } catch (NoSuchFieldException ex) {
            return null;
        }
        if (!Modifier.isStatic(field.getModifiers()) ||
                !Modifier.isFinal(field.getModifiers()) ||
                !Fetcher.class.isAssignableFrom(field.getType())
        ) {
            return null;
        }
        field.setAccessible(true);
        try {
            return (Fetcher<?>) field.get(null);
        } catch (IllegalAccessException ex) {
            throw new IllegalApiException(
                    "Cannot get `" +
                            fetchBy +
                            "` of \"" +
                            ownerType.getName() +
                            "\""
            );
        }
    }

    private static Fetcher<?> kotlinFetcher(String fetchBy, Class<?> ownerType) {
        Field companionField;
        try {
            companionField = ownerType.getDeclaredField("Companion");
        } catch (NoSuchFieldException ex) {
            companionField = null;
        }
        Object companion = null;
        Field field = null;
        if (companionField != null) {
            companionField.setAccessible(true);
            try {
                companion = companionField.get(null);
            } catch (IllegalAccessException ex) {
                // Do nothing
            }
            if (companion != null) {
                try {
                    field = companionField.getType().getDeclaredField(fetchBy);
                } catch (NoSuchFieldException ex) {
                    // Do nothing
                }
            }
        }
        if (field == null) {
            return null;
        }
        if (!Fetcher.class.isAssignableFrom(field.getType())) {
            throw new IllegalApiException(
                    "Illegal annotation @" +
                            FetchBy.class.getName() +
                            ", the field \"" +
                            field +
                            "\" must return fetcher"
            );
        }
        field.setAccessible(true);
        try {
            return (Fetcher<?>) field.get(companion);
        } catch (IllegalAccessException ex) {
            throw new IllegalApiException(
                    "Cannot get `" +
                            fetchBy +
                            "` of \"" +
                            ownerType.getName() +
                            "\""
            );
        }
    }

    @Override
    protected String toStringImpl(Set<Graph> stack) {
        return immutableType.toString() +
                '{' +
                properties.values().stream().map(it -> string(it, stack)).collect(Collectors.joining(", ")) +
                '}';
    }
}
