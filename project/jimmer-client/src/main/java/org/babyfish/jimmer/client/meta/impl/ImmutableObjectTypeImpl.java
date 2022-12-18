package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.IllegalDocMetaException;
import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.spi.EntityPropImplementor;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ImmutableObjectTypeImpl implements ImmutableObjectType {

    private final ImmutableType immutableType;

    private final Category category;

    private final Fetcher<?> fetcher;

    private final FetchByInfo fetchByInfo;

    private final Document document;

    private Map<String, Property> props;

    private ImmutableObjectTypeImpl(ImmutableType immutableType, Fetcher<?> fetcher, FetchByInfo fetchByInfo) {
        this.immutableType = immutableType;
        this.category = Category.FETCH;
        this.fetcher = fetcher;
        this.fetchByInfo = fetchByInfo;
        this.document = DocumentImpl.of(immutableType.getJavaClass());
    }

    private ImmutableObjectTypeImpl(ImmutableType immutableType, Category category) {
        this.immutableType = immutableType;
        this.category = category;
        this.fetcher = null;
        this.fetchByInfo = null;
        this.document = DocumentImpl.of(immutableType.getJavaClass());
    }

    private ImmutableObjectTypeImpl(ImmutableType immutableType, boolean anonymous, Category category, Map<String, Property> props) {
        this.immutableType = immutableType;
        this.category = category;
        this.fetcher = null;
        this.fetchByInfo = null;
        this.props = props;
        this.document = DocumentImpl.of(immutableType.getJavaClass());
    }

    @Override
    public Class<?> getJavaType() {
        return immutableType.getJavaClass();
    }

    @Override
    public boolean isEntity() {
        return immutableType.isEntity();
    }

    @Override
    public Map<String, Property> getProperties() {
        return props;
    }

    @Override
    public ImmutableType getImmutableType() {
        return immutableType;
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public FetchByInfo getFetchByInfo() {
        return fetchByInfo;
    }

    @Nullable
    @Override
    public Document getDocument() {
        return document;
    }

    Fetcher<?> getFetcher() {
        return fetcher;
    }

    FetchByInfo fetchByInfo() {
        return fetchByInfo;
    }

    @Override
    public void accept(Visitor visitor) {
        if (visitor.isTypeVisitable(this)) {
            visitor.visitImmutableObjectType(this);
            for (Property prop : props.values()) {
                prop.getType().accept(visitor);
            }
        }
    }

    @Override
    public String toString() {
        if (category == Category.VIEW) {
            return "@view:" + immutableType;
        }
        if (category == Category.RAW) {
            return "@raw:" + immutableType;
        }
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        boolean addComma = false;
        for (Property prop : props.values()) {
            if (addComma) {
                builder.append(", ");
            } else {
                addComma = true;
            }
            builder.append(prop.getName()).append(": ").append(prop.getType());
        }
        builder.append("}");
        return builder.toString();
    }

    static ImmutableObjectType fetch(Context ctx, ImmutableType immutableType, Fetcher<?> fetcher, FetchByInfo info) {
        ImmutableObjectTypeImpl impl;
        if (info != null) {
            impl = (ImmutableObjectTypeImpl) ctx.getImmutableObjectType(Category.FETCH, immutableType, fetcher);
            if (impl != null) {
                return impl;
            }
        }

        if (immutableType != fetcher.getImmutableType()) {
            throw new IllegalDocMetaException(
                    "Illegal " +
                            ctx.getLocation() +
                            ", @" +
                            FetchBy.class.getName() +
                            " specifies a fetcher whose type is \"" +
                            fetcher.getImmutableType() +
                            "\", but the decorated type is \"" +
                            immutableType +
                            "\""
            );
        }

        impl = new ImmutableObjectTypeImpl(fetcher.getImmutableType(), fetcher, info);
        if (info != null) {
            ctx.addImmutableObjectType(impl);
        }

        Map<String, Property> props = new LinkedHashMap<>();
        for (Field field : fetcher.getFieldMap().values()) {
            ImmutableProp prop = field.getProp();
            if (prop.isAssociation(TargetLevel.ENTITY)) {
                Type type = fetch(ctx, prop.getTargetType(), field.getChildFetcher(), null);
                if (prop.isNullable() && field.getRecursionStrategy() == null) {
                    type = NullableTypeImpl.of(type);
                }
                if (prop.isReferenceList(TargetLevel.ENTITY)) {
                    type = new ArrayTypeImpl(type);
                }
                props.put(prop.getName(), new PropertyImpl(prop.getName(), type, DocumentImpl.of(prop)));
            } else {
                props.put(prop.getName(), property(ctx, prop));
            }
        }
        impl.props = Collections.unmodifiableMap(props);
        return impl;
    }

    static ImmutableObjectType view(
            Context ctx,
            ImmutableType immutableType
    ) {
        ImmutableObjectTypeImpl impl = (ImmutableObjectTypeImpl) ctx.getImmutableObjectType(Category.VIEW, immutableType, null);
        if (impl != null) {
            return impl;
        }

        impl = new ImmutableObjectTypeImpl(immutableType, Category.VIEW);
        ctx.addImmutableObjectType(impl);

        Map<String, Property> props = new LinkedHashMap<>();
        for (ImmutableProp prop : immutableType.getProps().values()) {
            if (prop.getStorage() != null) {
                if (prop.isAssociation(TargetLevel.ENTITY)) {
                    Type type = idOnly(ctx, prop.getTargetType());
                    if (prop.isNullable()) {
                        type = NullableTypeImpl.of(type);
                    }
                    if (prop.isReferenceList(TargetLevel.ENTITY)) {
                        type = new ArrayTypeImpl(type);
                    }
                    props.put(prop.getName(), new PropertyImpl(prop.getName(), type, DocumentImpl.of(prop)));
                } else {
                    props.put(prop.getName(), property(ctx, prop));
                }
            }
        }
        impl.props = Collections.unmodifiableMap(props);
        return impl;
    }

    static ImmutableObjectType raw(
            Context ctx,
            ImmutableType immutableType
    ) {
        ImmutableObjectTypeImpl impl = (ImmutableObjectTypeImpl) ctx.getImmutableObjectType(Category.RAW, immutableType, null);
        if (impl != null) {
            return impl;
        }

        impl = new ImmutableObjectTypeImpl(immutableType, Category.RAW);
        ctx.addImmutableObjectType(impl);

        Map<String, Property> props = new LinkedHashMap<>();
        for (ImmutableProp prop : immutableType.getProps().values()) {
            props.put(prop.getName(), property(ctx, prop));
        }
        impl.props = Collections.unmodifiableMap(props);
        return impl;
    }

    static ObjectType idOnly(Context ctx, ImmutableType immutableType) {
        Map<String, Property> props = new LinkedHashMap<>();
        ImmutableProp idProp = immutableType.getIdProp();
        Type type = ctx.parseType(((EntityPropImplementor)idProp).getJavaGetter().getAnnotatedReturnType());
        props.put(idProp.getName(), new PropertyImpl(idProp.getName(), type, DocumentImpl.of(idProp)));
        return new ImmutableObjectTypeImpl(
                immutableType,
                true,
                Category.FETCH,
                Collections.unmodifiableMap(props)
        );
    }

    private static Property property(Context ctx, ImmutableProp prop) {
        Type type = ctx.parseType(((EntityPropImplementor)prop).getJavaGetter().getAnnotatedReturnType());
        if (prop.isNullable()) {
            type = NullableTypeImpl.of(type);
        }
        return new PropertyImpl(prop.getName(), type, DocumentImpl.of(prop));
    }
}
