package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.spi.EntityPropImplementor;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ImmutableObjectTypeImpl implements ImmutableObjectType {

    private final ImmutableType immutableType;

    private final Category category;

    private Map<String, Property> props;

    public ImmutableObjectTypeImpl(ImmutableType immutableType, Category category) {
        this.immutableType = immutableType;
        this.category = category;
    }

    public ImmutableObjectTypeImpl(ImmutableType immutableType, Category category, Map<String, Property> props) {
        this.immutableType = immutableType;
        this.category = category;
        this.props = props;
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

    static ImmutableObjectType fetch(Context ctx, Fetcher<?> fetcher) {
        Map<String, Property> props = new LinkedHashMap<>();
        for (Field field : fetcher.getFieldMap().values()) {
            ImmutableProp prop = field.getProp();
            if (prop.isAssociation(TargetLevel.ENTITY)) {
                Type type = fetch(ctx, field.getChildFetcher());
                if (prop.isNullable()) {
                    type = NullableTypeImpl.of(type);
                }
                if (prop.isReferenceList(TargetLevel.ENTITY)) {
                    type = new ArrayTypeImpl(type);
                }
                props.put(prop.getName(), new PropertyImpl(prop.getName(), type));
            } else {
                props.put(prop.getName(), property(ctx, prop));
            }
        }
        return new ImmutableObjectTypeImpl(
                fetcher.getImmutableType(),
                Category.FETCH,
                Collections.unmodifiableMap(props)
        );
    }

    static ImmutableObjectType view(
            Context ctx,
            ImmutableType immutableType
    ) {
        ImmutableObjectTypeImpl impl = new ImmutableObjectTypeImpl(immutableType, Category.VIEW);
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
                    props.put(prop.getName(), new PropertyImpl(prop.getName(), type));
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
        ImmutableObjectTypeImpl impl = new ImmutableObjectTypeImpl(immutableType, Category.RAW);
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
        props.put(idProp.getName(), new PropertyImpl(idProp.getName(), type));
        return new ImmutableObjectTypeImpl(
                immutableType,
                Category.FETCH,
                Collections.unmodifiableMap(props)
        );
    }

    private static Property property(Context ctx, ImmutableProp prop) {
        Type type = ctx.parseType(((EntityPropImplementor)prop).getJavaGetter().getAnnotatedReturnType());
        if (prop.isNullable()) {
            type = NullableTypeImpl.of(type);
        }
        return new PropertyImpl(prop.getName(), type);
    }
}
