package org.babyfish.jimmer.sql.association.meta;

import org.babyfish.jimmer.jackson.Converter;
import org.babyfish.jimmer.jackson.ConverterMetadata;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.association.Association;
import org.babyfish.jimmer.sql.meta.*;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public abstract class AssociationProp implements ImmutableProp {

    final AssociationType declaringType;

    AssociationProp(AssociationType declaringType) {
        this.declaringType = declaringType;
    }

    @Override
    public @NotNull AssociationType getDeclaringType() {
        return declaringType;
    }

    @Override
    public @NotNull ImmutablePropCategory getCategory() {
        return ImmutablePropCategory.REFERENCE;
    }

    @NotNull
    @Override
    public Class<?> getReturnClass() {
        return getElementClass();
    }

    @NotNull
    @Override
    public Type getGenericType() {
        return getElementClass();
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
    public boolean isInputNotNull() {
        return true;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Annotation getAssociationAnnotation() {
        return null;
    }

    @Override
    public Class<? extends Annotation> getPrimaryAnnotationType() {
        return ManyToOne.class;
    }

    @Override
    public TargetTransferMode getTargetTransferMode() {
        return TargetTransferMode.AUTO;
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
    public boolean isFormula() {
        return false;
    }

    @Override
    public boolean isTargetForeignKeyReal(MetadataStrategy strategy) {
        return true;
    }

    @Override
    public <S extends Storage> S getStorage(MetadataStrategy strategy) {
        return null;
    }

    @Override
    public SqlTemplate getSqlTemplate() {
        return null;
    }

    @Override
    public boolean isView() {
        return false;
    }

    @Override
    public ImmutableProp getIdViewProp() {
        return null;
    }

    @Override
    public ImmutableProp getIdViewBaseProp() {
        return null;
    }

    @Override
    public ImmutableProp getManyToManyViewBaseProp() {
        return null;
    }

    @Override
    public ImmutableProp getManyToManyViewBaseDeeperProp() {
        return null;
    }

    @Override
    public ConverterMetadata getConverterMetadata() {
        return null;
    }

    @Override
    public <S, T> Converter<S, T> getConverter() {
        return null;
    }

    @Override
    public <S, T> Converter<S, T> getConverter(boolean forList) {
        return null;
    }

    @Override
    public <S, T> Converter<S, T> getAssociatedIdConverter(boolean forList) {
        return null;
    }

    @Override
    public LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(SqlContext sqlContext) {
        return null;
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
    public boolean isLogicalDeleted() {
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
    public ImmutableProp getReal() {
        return this;
    }

    @Override
    public List<Dependency> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public List<ImmutableProp> getPropsDependOnSelf() {
        return Collections.emptyList();
    }

    @Override
    public Ref<Object> getDefaultValueRef() {
        return null;
    }

    @Override
    public boolean isExcludedFromAllScalars() {
        return false;
    }

    @Override
    public List<OrderedItem> getOrderedItems() {
        return Collections.emptyList();
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public ImmutableProp toOriginal() {
        return this;
    }

    @Override
    public boolean hasStorage() {
        return true;
    }

    @Override
    public boolean isColumnDefinition() {
        return true;
    }

    @Override
    public boolean isMiddleTableDefinition() {
        return false;
    }

    @Override
    public boolean isRecursive() {
        return false;
    }

    @Override
    public String toString() {
        return declaringType + "." + getName();
    }

    static class Source extends AssociationProp {

        private static final Method GETTER;

        Source(AssociationType declaringType) {
            super(declaringType);
        }

        @Override
        public PropId getId() {
            return PropId.byIndex(0);
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

        @Override
        public Annotation[] getAnnotations() {
            return GETTER.getAnnotations();
        }

        @SuppressWarnings("unchecked")
        @Override
        public ColumnDefinition getStorage(MetadataStrategy strategy) {
            return declaringType.getMiddleTable(strategy).getColumnDefinition();
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

        Target(AssociationType declaringType) {
            super(declaringType);
        }

        @Override
        public PropId getId() {
            return PropId.byIndex(1);
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

        @Override
        public Annotation[] getAnnotations() {
            return GETTER.getAnnotations();
        }

        @SuppressWarnings("unchecked")
        @Override
        public ColumnDefinition getStorage(MetadataStrategy strategy) {
            return declaringType.getMiddleTable(strategy).getTargetColumnDefinition();
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
