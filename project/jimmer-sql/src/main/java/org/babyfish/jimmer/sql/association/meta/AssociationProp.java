package org.babyfish.jimmer.sql.association.meta;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutablePropCategory;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.CascadeAction;
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
    public CascadeAction getDeleteAction() {
        return CascadeAction.NONE;
    }

    @Override
    public boolean isAssociation() {
        return true;
    }

    @Override
    public boolean isReference() {
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
    public boolean isEntityList() {
        return false;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return getGetter().getAnnotation(annotationType);
    }

    @Override
    public <A extends Annotation> A[] getAnnotations(Class<A> annotationType) {
        return getGetter().getAnnotationsByType(annotationType);
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

        private Column column;

        Source(AssociationType declaringType) {
            super(declaringType);
            column = new Column(declaringType.getMiddleTable().getJoinColumnName());
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
        public Method getGetter() {
            return GETTER;
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

        private Column column;

        Target(AssociationType declaringType) {
            super(declaringType);
            column = new Column(declaringType.getMiddleTable().getTargetJoinColumnName());
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
        public Method getGetter() {
            return GETTER;
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
