package org.babyfish.jimmer.sql.ddl.fake;

import org.babyfish.jimmer.jackson.Converter;
import org.babyfish.jimmer.jackson.ConverterMetadata;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.meta.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author honhimW
 */
 
public class FakeImmutablePropImpl implements ImmutableProp {
    
    public ImmutableType declaringType;
    
    @Override
    public @NotNull ImmutableType getDeclaringType() {
        return declaringType;
    }
    
    public PropId id;

    @Override
    public PropId getId() {
        return id;
    }
    
    public String name;

    @Override
    public @NotNull String getName() {
        return name;
    }
    
    public ImmutablePropCategory category;

    @Override
    public @NotNull ImmutablePropCategory getCategory() {
        return category;
    }
    
    public Class<?> elementClass;

    @Override
    public @NotNull Class<?> getElementClass() {
        return elementClass;
    }
    
    public Class<?> returnClass;

    @Override
    public @NotNull Class<?> getReturnClass() {
        return returnClass;
    }
    
    public Type genericType;

    @Override
    public @NotNull Type getGenericType() {
        return genericType;
    }

    public boolean isEmbedded;

    @Override
    public boolean isEmbedded(EmbeddedLevel level) {
        return isEmbedded;
    }

    public boolean isScalar;

    @Override
    public boolean isScalar(TargetLevel level) {
        return isScalar;
    }

    public boolean isScalarList;

    @Override
    public boolean isScalarList() {
        return isScalarList;
    }

    public boolean isAssociation;

    @Override
    public boolean isAssociation(TargetLevel level) {
        return isAssociation;
    }

    public boolean isReference;

    @Override
    public boolean isReference(TargetLevel level) {
        return isReference;
    }

    public boolean isReferenceList;

    @Override
    public boolean isReferenceList(TargetLevel level) {
        return isReferenceList;
    }

    public boolean isNullable;

    @Override
    public boolean isNullable() {
        return isNullable;
    }

    public boolean isInputNotNull;

    @Override
    public boolean isInputNotNull() {
        return isInputNotNull;
    }

    public boolean isMutable;

    @Override
    public boolean isMutable() {
        return isMutable;
    }

    public Annotation[] annotations;

    @SuppressWarnings("unchecked")
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotationType == annotation.annotationType()) {
                    return (A) annotation;
                }
            }
        }
        return null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends Annotation> A[] getAnnotations(Class<A> annotationType) {
        if (annotations != null) {
            List<A> list = new ArrayList<>();
            for (Annotation annotation : annotations) {
                if (annotationType == annotation.annotationType()) {
                    list.add((A) annotation);
                }
            }
            list.toArray((A[]) Array.newInstance(annotationType, list.size()));
        }
        return null;
    }

    public Annotation associationAnnotation;

    @Override
    public Annotation getAssociationAnnotation() {
        return associationAnnotation;
    }

    public Class<? extends Annotation> primaryAnnotationType;

    @Override
    public Class<? extends Annotation> getPrimaryAnnotationType() {
        return primaryAnnotationType;
    }

    public boolean isTransient;

    @Override
    public boolean isTransient() {
        return isTransient;
    }

    public boolean hasTransientResolver;

    @Override
    public boolean hasTransientResolver() {
        return hasTransientResolver;
    }

    public boolean isFormula;

    @Override
    public boolean isFormula() {
        return isFormula;
    }

    public boolean isTargetForeignKeyReal;

    @Override
    public boolean isTargetForeignKeyReal(MetadataStrategy strategy) {
        return isTargetForeignKeyReal;
    }

    public TargetTransferMode targetTransferMode;

    @Override
    public TargetTransferMode getTargetTransferMode() {
        return targetTransferMode;
    }

    public SqlTemplate sqlTemplate;

    @Override
    public @Nullable SqlTemplate getSqlTemplate() {
        return sqlTemplate;
    }

    public boolean isView;

    @Override
    public boolean isView() {
        return isView;
    }

    public ImmutableProp idViewProp;

    @Override
    public ImmutableProp getIdViewProp() {
        return idViewProp;
    }

    public ImmutableProp idViewBaseProp;

    @Override
    public ImmutableProp getIdViewBaseProp() {
        return idViewBaseProp;
    }

    public ImmutableProp manyToManyViewBaseProp;

    @Override
    public ImmutableProp getManyToManyViewBaseProp() {
        return manyToManyViewBaseProp;
    }

    public ImmutableProp manyToManyViewBaseDeeperProp;

    @Override
    public ImmutableProp getManyToManyViewBaseDeeperProp() {
        return manyToManyViewBaseDeeperProp;
    }

    public ConverterMetadata converterMetadata;

    @Override
    public ConverterMetadata getConverterMetadata() {
        return converterMetadata;
    }

    public Converter<?, ?> converter;

    @SuppressWarnings("unchecked")
    @Override
    public <S, T> Converter<S, T> getConverter() {
        return (Converter<S, T>) converter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S, T> Converter<S, T> getConverter(boolean forList) {
        return (Converter<S, T>) converter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S, T> Converter<S, T> getAssociatedIdConverter(boolean forList) {
        return (Converter<S, T>) converter;
    }

    public DissociateAction dissociateAction;

    @Override
    public @NotNull DissociateAction getDissociateAction() {
        return dissociateAction;
    }

    public boolean hasStorage;

    @Override
    public boolean hasStorage() {
        return hasStorage;
    }

    public boolean isColumnDefinition;

    @Override
    public boolean isColumnDefinition() {
        return isColumnDefinition;
    }

    public boolean isMiddleTableDefinition;

    @Override
    public boolean isMiddleTableDefinition() {
        return isMiddleTableDefinition;
    }

    public boolean isRecursive;

    @Override
    public boolean isRecursive() {
        return isRecursive;
    }

    public Storage storage;

    @SuppressWarnings("unchecked")
    @Override
    public <S extends Storage> S getStorage(MetadataStrategy strategy) {
        return (S) strategy;
    }

    public LogicalDeletedValueGenerator<?> logicalDeletedValueGenerator;

    @Override
    public LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(SqlContext sqlContext) {
        return logicalDeletedValueGenerator;
    }

    public boolean isId;

    @Override
    public boolean isId() {
        return isId;
    }

    public boolean isVersion;

    @Override
    public boolean isVersion() {
        return isVersion;
    }

    public boolean isLogicalDeleted;

    @Override
    public boolean isLogicalDeleted() {
        return isLogicalDeleted;
    }

    public ImmutableType targetType;

    @Override
    public ImmutableType getTargetType() {
        return targetType;
    }

    public List<OrderedItem> orderedItems;

    @Override
    public List<OrderedItem> getOrderedItems() {
        return orderedItems;
    }

    public ImmutableProp mappedBy;

    @Override
    public ImmutableProp getMappedBy() {
        return mappedBy;
    }

    public ImmutableProp opposite;

    @Override
    public ImmutableProp getOpposite() {
        return opposite;
    }

    public ImmutableProp real;

    @Override
    public ImmutableProp getReal() {
        return real;
    }

    public List<Dependency> dependencies;

    @Override
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public List<ImmutableProp> propsDependOnSelf;

    @Override
    public List<ImmutableProp> getPropsDependOnSelf() {
        return propsDependOnSelf;
    }

    public Ref<Object> defaultValueRef;

    @Override
    public Ref<Object> getDefaultValueRef() {
        return defaultValueRef;
    }

    public boolean isExcludedFromAllScalars;

    @Override
    public boolean isExcludedFromAllScalars() {
        return isExcludedFromAllScalars;
    }

    public boolean isRemote;

    @Override
    public boolean isRemote() {
        return isRemote;
    }

    public ImmutableProp original;

    @Override
    public ImmutableProp toOriginal() {
        return original;
    }
}
