package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.meta.Storage;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.List;

public abstract class RedirectedProp implements ImmutableProp {

    private final ImmutableProp raw;

    private RedirectedProp(ImmutableProp raw) {
        this.raw = raw;
    }

    public static ImmutableProp source(ImmutableProp prop, ImmutableType sourceType) {
        if (prop.getDeclaringType() == sourceType) {
            return prop;
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
        return new RedirectedProp(prop) {
            @NotNull
            @Override
            public ImmutableType getDeclaringType() {
                return sourceType;
            }
        };
    }

    public static ImmutableProp target(ImmutableProp prop, ImmutableType targetType) {
        if (prop.getTargetType() == targetType) {
            return prop;
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
        return new RedirectedProp(prop) {
            @Override
            public ImmutableType getTargetType() {
                return targetType;
            }
        };
    }

    @Override
    @NotNull
    public ImmutableType getDeclaringType() {
        return raw.getDeclaringType();
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
    public ImmutableType getTargetType() {
        return raw.getTargetType();
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
}
