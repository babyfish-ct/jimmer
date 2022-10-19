package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.meta.Storage;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;

public class RedirectedProp implements ImmutableProp {

    private final ImmutableProp raw;

    private final ImmutableType sourceType;

    private final ImmutableType targetType;

    private RedirectedProp(ImmutableProp prop, ImmutableType sourceType, ImmutableType targetType) {
        this.raw = unwrap(prop);
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    public static ImmutableProp source(ImmutableProp prop, Class<?> sourceType) {
        return source(prop, ImmutableType.get(sourceType));
    }

    public static ImmutableProp source(ImmutableProp prop, ImmutableType sourceType) {
        if (prop == null) {
            return null;
        }
        if (!prop.getDeclaringType().isAssignableFrom(sourceType)) {
            throw new IllegalArgumentException(
                    "Cannot redirect source type of \"" +
                            prop +
                            "\" to \"" +
                            sourceType +
                            "\" because that class not derived type of \"" +
                            prop.getDeclaringType() +
                            "\""
            );
        }
        if (prop.getDeclaringType() == sourceType) {
            return prop;
        }
        return new RedirectedProp(
                prop,
                sourceType,
                prop.getTargetType()
        );
    }

    public static ImmutableProp target(ImmutableProp prop, Class<?> targetType) {
        return target(prop, ImmutableType.get(targetType));
    }

    public static ImmutableProp target(ImmutableProp prop, ImmutableType targetType) {
        if (prop == null) {
            return null;
        }
        if (prop.getTargetType() == null) {
            throw new IllegalArgumentException(
                    "Cannot redirect target type of \"" +
                            prop +
                            "\" because it is not association property\""
            );
        }
        if (!prop.getTargetType().isAssignableFrom(targetType)) {
            throw new IllegalArgumentException(
                    "Cannot redirect target type of \"" +
                            prop +
                            "\" to \"" +
                            targetType +
                            "\" because that class not derived type of \"" +
                            prop.getTargetType() +
                            "\""
            );
        }
        if (prop.getTargetType() == targetType) {
            return prop;
        }
        return new RedirectedProp(
                prop,
                prop.getDeclaringType(),
                targetType
        );
    }

    @Override
    public ImmutableType getDeclaringType() {
        return sourceType;
    }

    @Override
    public ImmutableType getTargetType() {
        return targetType;
    }

    @Override
    public int getId() {
        return raw.getId();
    }

    @Override
    @NotNull
    public String getName() {
        return raw.getName();
    }

    @Override
    @NotNull
    public ImmutablePropCategory getCategory() {
        return raw.getCategory();
    }

    @Override
    @NotNull
    public Class<?> getElementClass() {
        return raw.getElementClass();
    }

    @Override
    public boolean isScalar() {
        return raw.isScalar();
    }

    @Override
    public boolean isScalarList() {
        return raw.isScalarList();
    }

    @Override
    public boolean isAssociation(TargetLevel level) {
        return raw.isAssociation(level);
    }

    @Override
    public boolean isReference(TargetLevel level) {
        return raw.isReference(level);
    }

    @Override
    public boolean isReferenceList(TargetLevel level) {
        return raw.isReferenceList(level);
    }

    @Override
    public boolean isNullable() {
        return raw.isNullable();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return raw.getAnnotation(annotationType);
    }

    @Override
    public <A extends Annotation> A[] getAnnotations(Class<A> annotationType) {
        return raw.getAnnotations(annotationType);
    }

    @Override
    public Annotation getAssociationAnnotation() {
        return raw.getAssociationAnnotation();
    }

    @Override
    public boolean isTransient() {
        return raw.isTransient();
    }

    @Override
    public boolean hasTransientResolver() {
        return raw.hasTransientResolver();
    }

    @Override
    @NotNull
    public DissociateAction getDissociateAction() {
        return raw.getDissociateAction();
    }

    @Override
    public <S extends Storage> S getStorage() {
        return raw.getStorage();
    }

    @Override
    public boolean isId() {
        return raw.isId();
    }

    @Override
    public boolean isVersion() {
        return raw.isVersion();
    }

    @Override
    public List<OrderedItem> getOrderedItems() {
        return raw.getOrderedItems();
    }

    @Override
    public ImmutableProp getMappedBy() {
        return raw.getMappedBy();
    }

    @Override
    public ImmutableProp getOpposite() {
        return raw.getOpposite();
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(raw);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ImmutableProp)) {
            return false;
        }
        return raw == unwrap((ImmutableProp) o);
    }

    @Override
    public String toString() {
        return sourceType + "." + raw.getName();
    }

    static ImmutableProp unwrap(ImmutableProp prop) {
        return prop instanceof RedirectedProp ?
                ((RedirectedProp) prop).raw :
                prop;
    }
}
