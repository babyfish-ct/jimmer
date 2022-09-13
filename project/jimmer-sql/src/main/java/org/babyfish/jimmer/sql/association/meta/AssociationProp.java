package org.babyfish.jimmer.sql.association.meta;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutablePropCategory;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.association.Association;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.meta.Storage;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public abstract class AssociationProp implements ImmutableProp {

    final AssociationType declaringType;

    AssociationProp(AssociationType declaringType) {
        this.declaringType = declaringType;
    }

    @Override
    public ImmutableType getDeclaringType() {
        return declaringType;
    }

    @Override
    public ImmutablePropCategory getCategory() {
        return ImmutablePropCategory.REFERENCE;
    }

    @Override
    public DissociateAction getDissociateAction() {
        return DissociateAction.NONE;
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
    public boolean isScalar() {
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
    public String toString() {
        return declaringType + "." + getName();
    }

    static class Source extends AssociationProp {

        private static final Method GETTER;

        private final Column column;

        Source(AssociationType declaringType) {
            super(declaringType);
            column = new Column(declaringType.getMiddleTable().getJoinColumnName());
        }

        @Override
        public int getId() {
            return 1;
        }

        @Override
        public String getName() {
            return "source";
        }

        @Override
        public Class<?> getElementClass() {
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
        public <S extends Storage> S getStorage() {
            return (S)column;
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

        private final Column column;

        Target(AssociationType declaringType) {
            super(declaringType);
            column = new Column(declaringType.getMiddleTable().getTargetJoinColumnName());
        }

        @Override
        public int getId() {
            return 2;
        }

        @Override
        public String getName() {
            return "target";
        }

        @Override
        public Class<?> getElementClass() {
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
        public <S extends Storage> S getStorage() {
            return (S)column;
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
