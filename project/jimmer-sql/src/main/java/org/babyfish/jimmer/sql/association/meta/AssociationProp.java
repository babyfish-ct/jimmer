package org.babyfish.jimmer.sql.association.meta;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.association.Association;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public abstract class AssociationProp implements ImmutableProp {

    final AssociationType declaringType;

    AssociationProp(AssociationType declaringType) {
        this.declaringType = declaringType;
    }

    @Override
    public @NotNull ImmutableType getDeclaringType() {
        return declaringType;
    }

    @Override
    public @NotNull ImmutablePropCategory getCategory() {
        return ImmutablePropCategory.REFERENCE;
    }

    @Override
    public @NotNull DissociateAction getDissociateAction() {
        return DissociateAction.NONE;
    }

    @Override
    public boolean isEmbedded(EmbeddedLevel level) {
        return level.hasReference() && getTargetType().getIdProp().isEmbedded(EmbeddedLevel.SCALAR);
    }

    @Override
    public boolean isAssociation(TargetLevel level) {
        return true;
    }

    @Override
    public boolean isReference(TargetLevel level) {
        return true;
    }

    @Override
    public boolean isScalar(TargetLevel level) {
        return false;
    }

    @Override
    public boolean isScalarList() {
        return false;
    }

    @Override
    public boolean isReferenceList(TargetLevel level) {
        return false;
    }

    @Override
    public boolean isNullable() {
        return false;
    }
    
    @Override
    public Annotation getAssociationAnnotation() {
        return null;
    }

    @Override
    public boolean isTransient() {
        return false;
    }

    @Override
    public boolean hasTransientResolver() {
        return false;
    }

    @Override
    public boolean isId() {
        return false;
    }

    @Override
    public boolean isVersion() {
        return false;
    }

    @Override
    public ImmutableProp getMappedBy() {
        return null;
    }

    @Override
    public ImmutableProp getOpposite() {
        return null;
    }

    @Override
    public List<OrderedItem> getOrderedItems() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return declaringType + "." + getName();
    }

    static class Source extends AssociationProp {

        private static final Method GETTER;

        private final ColumnDefinition definition;

        Source(AssociationType declaringType) {
            super(declaringType);
            definition = declaringType.getMiddleTable().getColumnDefinition();
        }

        @Override
        public int getId() {
            return 1;
        }

        @Override
        public @NotNull String getName() {
            return "source";
        }

        @Override
        public @NotNull Class<?> getElementClass() {
            return declaringType.getSourceType().getJavaClass();
        }

        @Override
        public ImmutableType getTargetType() {
            return declaringType.getSourceType();
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            return GETTER.getAnnotation(annotationType);
        }

        @Override
        public <A extends Annotation> A[] getAnnotations(Class<A> annotationType) {
            return GETTER.getAnnotationsByType(annotationType);
        }

        @SuppressWarnings("unchecked")
        @Override
        public ColumnDefinition getStorage() {
            return definition;
        }

        static {
            try {
                GETTER = Association.class.getMethod("source");
            } catch (NoSuchMethodException ex) {
                throw new AssertionError("Internal bug: Cannot get Association.source");
            }
        }
    }

    static class Target extends AssociationProp {

        private static final Method GETTER;

        private final ColumnDefinition definition;

        Target(AssociationType declaringType) {
            super(declaringType);
            definition = declaringType.getMiddleTable().getTargetColumnDefinition();
        }

        @Override
        public int getId() {
            return 2;
        }

        @Override
        public @NotNull String getName() {
            return "target";
        }

        @Override
        public @NotNull Class<?> getElementClass() {
            return declaringType.getTargetType().getJavaClass();
        }

        @Override
        public ImmutableType getTargetType() {
            return declaringType.getTargetType();
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            return GETTER.getAnnotation(annotationType);
        }

        @Override
        public <A extends Annotation> A[] getAnnotations(Class<A> annotationType) {
            return GETTER.getAnnotationsByType(annotationType);
        }

        @SuppressWarnings("unchecked")
        @Override
        public ColumnDefinition getStorage() {
            return definition;
        }

        static {
            try {
                GETTER = Association.class.getMethod("target");
            } catch (NoSuchMethodException ex) {
                throw new AssertionError("Internal bug: Cannot get Association.source");
            }
        }
    }
}
