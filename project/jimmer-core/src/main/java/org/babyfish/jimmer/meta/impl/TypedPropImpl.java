package org.babyfish.jimmer.meta.impl;

import org.apache.commons.lang3.reflect.Typed;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.meta.Storage;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;

public class TypedPropImpl<S, T> implements TypedProp<S, T> {

    private final ImmutableProp prop;

    protected TypedPropImpl(ImmutableProp prop) {
        this.prop = prop;
    }

    @Override
    @NotNull
    public ImmutableType getDeclaringType() {
        return prop.getDeclaringType();
    }

    @Override
    public int getId() {
        return prop.getId();
    }

    @Override
    @NotNull
    public String getName() {
        return prop.getName();
    }

    @Override
    @NotNull
    public ImmutablePropCategory getCategory() {
        return prop.getCategory();
    }

    @Override
    @NotNull
    public Class<?> getElementClass() {
        return prop.getElementClass();
    }

    @Override
    public boolean isScalar() {
        return prop.isScalar();
    }

    @Override
    public boolean isScalarList() {
        return prop.isScalarList();
    }

    @Override
    public boolean isAssociation(TargetLevel level) {
        return prop.isAssociation(level);
    }

    @Override
    public boolean isReference(TargetLevel level) {
        return prop.isReference(level);
    }

    @Override
    public boolean isReferenceList(TargetLevel level) {
        return prop.isReferenceList(level);
    }

    @Override
    public boolean isNullable() {
        return prop.isNullable();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return prop.getAnnotation(annotationType);
    }

    @Override
    public <A extends Annotation> A[] getAnnotations(Class<A> annotationType) {
        return prop.getAnnotations(annotationType);
    }

    @Override
    public Annotation getAssociationAnnotation() {
        return prop.getAssociationAnnotation();
    }

    @Override
    public boolean isTransient() {
        return prop.isTransient();
    }

    @Override
    public boolean hasTransientResolver() {
        return prop.hasTransientResolver();
    }

    @Override
    @NotNull
    public DissociateAction getDissociateAction() {
        return prop.getDissociateAction();
    }

    @Override
    public <S extends Storage> S getStorage() {
        return prop.getStorage();
    }

    @Override
    public boolean isId() {
        return prop.isId();
    }

    @Override
    public boolean isVersion() {
        return prop.isVersion();
    }

    @Override
    public ImmutableType getTargetType() {
        return prop.getTargetType();
    }

    @Override
    public ImmutableProp getMappedBy() {
        return prop.getMappedBy();
    }

    @Override
    public ImmutableProp getOpposite() {
        return prop.getOpposite();
    }

    public static class Scalar<S, T> extends TypedPropImpl<S, T> implements TypedProp.Scalar<S, T> {

        public Scalar(ImmutableProp prop) {
            super(prop);
            if (!prop.isScalar()) {
                throw new IllegalArgumentException(
                        "\"" +
                                prop +
                                "\" is not scalar property"
                );
            }
        }
    }

    public static class ScalarList<S, T> extends TypedPropImpl<S, T> implements TypedProp.ScalarList<S, T> {

        public ScalarList(ImmutableProp prop) {
            super(prop);
            if (!prop.isScalarList()) {
                throw new IllegalArgumentException(
                        "\"" +
                                prop +
                                "\" is not scalar list property"
                );
            }
        }
    }

    public static class Reference<S, T> extends TypedPropImpl<S, T> implements TypedProp.Reference<S, T> {

        public Reference(ImmutableProp prop) {
            super(prop);
            if (!prop.isReference(TargetLevel.OBJECT)) {
                throw new IllegalArgumentException(
                        "\"" +
                                prop +
                                "\" is not reference property"
                );
            }
        }
    }

    public static class ReferenceList<S, T> extends TypedPropImpl<S, T> implements TypedProp.ReferenceList<S, T> {

        public ReferenceList(ImmutableProp prop) {
            super(prop);
            if (!prop.isReferenceList(TargetLevel.OBJECT)) {
                throw new IllegalArgumentException(
                        "\"" +
                                prop +
                                "\" is not reference list property"
                );
            }
        }
    }
}
