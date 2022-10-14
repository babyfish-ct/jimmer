package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.meta.Storage;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;

public abstract class RedirectedProp implements ImmutableProp {

    protected final ImmutableProp raw;

    RedirectedProp(ImmutableProp raw) {
        this.raw = raw;
    }

    public static ImmutableProp source(ImmutableProp prop, ImmutableType sourceType) {
        if (prop.getDeclaringType() == sourceType) {
            return prop;
        }
        return new SourceRedirectedProp(prop, sourceType);
    }

    public static ImmutableProp target(ImmutableProp prop, ImmutableType targetType) {
        if (prop.getTargetType() == targetType) {
            return prop;
        }
        return new TargetRedirectedProp(prop, targetType);
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

    private static class SourceRedirectedProp extends RedirectedProp {

        private final ImmutableType sourceType;

        private SourceRedirectedProp(ImmutableProp raw, ImmutableType sourceType) {
            super(raw);
            if (!raw.getDeclaringType().isAssignableFrom(sourceType)) {
                throw new IllegalArgumentException(
                        "Cannot redirect source type of \"" +
                                raw +
                                "\" to \"" +
                                sourceType +
                                "\" because that class not derived type of \"" +
                                raw.getDeclaringType() +
                                "\""
                );
            }
            this.sourceType = sourceType;
        }

        @NotNull
        @Override
        public ImmutableType getDeclaringType() {
            return sourceType;
        }

        @Override
        public int hashCode() {
            return raw.hashCode() ^ sourceType.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SourceRedirectedProp)) {
                return false;
            }
            SourceRedirectedProp other = (SourceRedirectedProp) obj;
            return raw.equals(other.raw) && sourceType.equals(other.sourceType);
        }

        @Override
        public String toString() {
            return sourceType + "." + raw.getName();
        }
    }

    private static class TargetRedirectedProp extends RedirectedProp {

        private final ImmutableType targetType;

        private TargetRedirectedProp(ImmutableProp raw, ImmutableType targetType) {
            super(raw);
            if (!raw.getTargetType().isAssignableFrom(targetType)) {
                throw new IllegalArgumentException(
                        "Cannot redirect target type of \"" +
                                raw +
                                "\" to \"" +
                                targetType +
                                "\" because that class not derived type of \"" +
                                raw.getTargetType() +
                                "\""
                );
            }
            this.targetType = targetType;
        }

        @Override
        public ImmutableType getTargetType() {
            return targetType;
        }

        @Override
        public int hashCode() {
            return raw.hashCode() ^ targetType.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TargetRedirectedProp)) {
                return false;
            }
            TargetRedirectedProp other = (TargetRedirectedProp) obj;
            return raw.equals(other.raw) && targetType.equals(other.targetType);
        }

        @Override
        public String toString() {
            return raw.toString();
        }
    }
}
