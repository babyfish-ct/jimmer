package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.jackson.Converter;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.babyfish.jimmer.sql.meta.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.List;

public interface ImmutableProp {

    @NotNull
    ImmutableType getDeclaringType();

    int getId();

    @NotNull
    String getName();

    @NotNull
    ImmutablePropCategory getCategory();

    @NotNull
    Class<?> getElementClass();

    boolean isEmbedded(EmbeddedLevel level);

    boolean isScalar(TargetLevel level);

    boolean isScalarList();

    boolean isAssociation(TargetLevel level);

    boolean isReference(TargetLevel level);

    boolean isReferenceList(TargetLevel level);

    boolean isNullable();

    boolean isInputNotNull();

    <A extends Annotation> A getAnnotation(Class<A> annotationType);

    Annotation[] getAnnotations();

    <A extends Annotation> A[] getAnnotations(Class<A> annotationType);

    Annotation getAssociationAnnotation();

    boolean isTransient();

    boolean hasTransientResolver();

    boolean isFormula();

    @Nullable
    FormulaTemplate getFormulaTemplate();

    Converter<?> getConverter();

    @NotNull
    DissociateAction getDissociateAction();

    @SuppressWarnings("unchecked")
    <S extends Storage> S getStorage();

    boolean isId();

    boolean isVersion();

    ImmutableType getTargetType();

    List<OrderedItem> getOrderedItems();

    ImmutableProp getMappedBy();

    ImmutableProp getOpposite();

    List<ImmutableProp> getDependencies();
}
