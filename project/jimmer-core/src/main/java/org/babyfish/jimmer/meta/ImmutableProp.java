package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.sql.DeleteAction;
import org.babyfish.jimmer.sql.meta.Storage;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface ImmutableProp {

    ImmutableType getDeclaringType();

    String getName();

    ImmutablePropCategory getCategory();

    Class<?> getElementClass();

    boolean isScalar();

    boolean isScalarList();

    boolean isAssociation();

    boolean isReference();

    boolean isEntityList();

    boolean isNullable();

    Method getGetter();

    <A extends Annotation> A getAnnotation(Class<A> annotationType);

    <A extends Annotation> A[] getAnnotations(Class<A> annotationType);

    Annotation getAssociationAnnotation();

    boolean isTransient();

    DeleteAction getDeleteAction();

    @SuppressWarnings("unchecked")
    <S extends Storage> S getStorage();

    boolean isId();

    boolean isVersion();

    ImmutableType getTargetType();

    ImmutableProp getMappedBy();

    ImmutableProp getOpposite();
}
