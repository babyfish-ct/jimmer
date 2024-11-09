package org.babyfish.jimmer.meta;

import kotlin.UByte;
import kotlin.UInt;
import kotlin.ULong;
import kotlin.UShort;
import org.babyfish.jimmer.jackson.Converter;
import org.babyfish.jimmer.jackson.ConverterMetadata;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.meta.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

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

    default boolean isUnsigned() {
        final String name = getElementClass().getName();
        return name.equals(ULong.class.getName()) ||
                name.equals(UInt.class.getName()) ||
                name.equals(UShort.class.getName()) ||
                name.equals(UByte.class.getName());
    }

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

    <S extends Storage> S getStorage(MetadataStrategy strategy);

    LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(SqlContext sqlContext);

    boolean isId();

    boolean isVersion();

    boolean isLogicalDeleted();

    ImmutableType getTargetType();

    List<OrderedItem> getOrderedItems();

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
