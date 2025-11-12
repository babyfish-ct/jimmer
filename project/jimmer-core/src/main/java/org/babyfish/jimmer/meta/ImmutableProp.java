package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.jackson.Converter;
import org.babyfish.jimmer.jackson.ConverterMetadata;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.meta.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public interface ImmutableProp {

    @NotNull
    ImmutableType getDeclaringType();

    PropId getId();

    @NotNull
    String getName();

    @NotNull
    ImmutablePropCategory getCategory();

    @NotNull
    Class<?> getElementClass();

    @NotNull
    Class<?> getReturnClass();

    @NotNull
    Type getGenericType();

    boolean isEmbedded(EmbeddedLevel level);

    boolean isScalar(TargetLevel level);

    boolean isScalarList();

    boolean isAssociation(TargetLevel level);

    boolean isReference(TargetLevel level);

    boolean isReferenceList(TargetLevel level);

    boolean isNullable();

    boolean isInputNotNull();

    boolean isMutable();

    <A extends Annotation> A getAnnotation(Class<A> annotationType);

    Annotation[] getAnnotations();

    <A extends Annotation> A[] getAnnotations(Class<A> annotationType);

    Annotation getAssociationAnnotation();

    Class<? extends Annotation> getPrimaryAnnotationType();

    boolean isTransient();

    boolean hasTransientResolver();

    boolean isFormula();

    boolean isTargetForeignKeyReal(MetadataStrategy strategy);

    TargetTransferMode getTargetTransferMode();

    @Nullable
    SqlTemplate getSqlTemplate();

    boolean isView();

    ImmutableProp getIdViewProp();

    ImmutableProp getIdViewBaseProp();

    ImmutableProp getManyToManyViewBaseProp();

    ImmutableProp getManyToManyViewBaseDeeperProp();

    ConverterMetadata getConverterMetadata();

    <S, T> Converter<S, T> getConverter();

    <S, T> Converter<S, T> getConverter(boolean forList);

    <S, T> Converter<S, T> getAssociatedIdConverter(boolean forList);

    @NotNull
    DissociateAction getDissociateAction();

    boolean hasStorage();

    boolean isColumnDefinition();

    boolean isMiddleTableDefinition();

    boolean isRecursive();

    <S extends Storage> S getStorage(MetadataStrategy strategy);

    LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(SqlContext sqlContext);

    boolean isId();

    boolean isVersion();

    boolean isLogicalDeleted();

    ImmutableType getTargetType();

    List<OrderedItem> getOrderedItems();

    @Nullable
    default Comparator<?> getComparator() {
        List<OrderedItem> orderItems = getOrderedItems();
        if (orderItems.isEmpty()) {
            return null;
        }
        return new Comparator<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public int compare(Object o1, Object o2) {
                if (o1 == o2) {
                    return 0;
                }
                if (o1 == null) {
                    return -1;
                }
                if (o2 == null) {
                    return +1;
                }
                ImmutableSpi spi1 = (ImmutableSpi) o1;
                ImmutableSpi spi2 = (ImmutableSpi) o2;
                for (OrderedItem orderedItem : orderItems) {
                    PropId propId = orderedItem.getProp().getId();
                    Comparator<?> comparator = orderedItem.isDesc() ? Comparator.reverseOrder() : Comparator.naturalOrder();
                    int cmp = ((Comparator<Object>)comparator).compare(spi1.__get(propId), spi2.__get(propId));
                    if (cmp != 0) {
                        return cmp;
                    }
                }
                return 0;
            }
        };
    }

    ImmutableProp getMappedBy();

    ImmutableProp getOpposite();

    /**
     * @return {@code mappedBy != null ? mappedBy : this}
     */
    ImmutableProp getReal();

    List<Dependency> getDependencies();

    List<ImmutableProp> getPropsDependOnSelf();

    Ref<Object> getDefaultValueRef();

    boolean isExcludedFromAllScalars();

    boolean isRemote();

    ImmutableProp toOriginal();
}
