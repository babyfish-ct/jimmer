package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.meta.Storage;
import org.jetbrains.annotations.NotNull;

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

    <A extends Annotation> A getAnnotation(Class<A> annotationType);

    <A extends Annotation> A[] getAnnotations(Class<A> annotationType);

    Annotation getAssociationAnnotation();

    boolean isTransient();

    boolean hasTransientResolver();

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
}
