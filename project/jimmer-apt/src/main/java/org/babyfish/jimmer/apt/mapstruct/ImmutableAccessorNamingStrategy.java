package org.babyfish.jimmer.apt.mapstruct;

import org.babyfish.jimmer.DraftConsumer;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.mapstruct.ap.spi.DefaultAccessorNamingStrategy;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class ImmutableAccessorNamingStrategy extends DefaultAccessorNamingStrategy {

    private static final String IMMUTABLE_TYPE_NAME = Immutable.class.getName();

    private static final String ENTITY_TYPE_NAME = Entity.class.getName();

    private static final String MAPPED_SUPERCLASS_TYPE_NAME = MappedSuperclass.class.getName();

    private static final String EMBEDDABLE_TYPE_NAME = Embeddable.class.getName();

    private static final String DRAFT_CONSUMER_TYPE_NAME = DraftConsumer.class.getName();

    @Override
    public String getPropertyName(ExecutableElement getterOrSetterMethod) {
        if (isRecordStyleGetter(getterOrSetterMethod)) {
            return getterOrSetterMethod.getSimpleName().toString();
        }
        return super.getPropertyName(getterOrSetterMethod);
    }

    @Override
    public boolean isGetterMethod(ExecutableElement method) {
        return super.isGetterMethod(method) || isRecordStyleGetter(method);
    }

    private boolean isRecordStyleGetter(ExecutableElement method) {
        if (method.getModifiers().contains(Modifier.STATIC) ||
                method.getReturnType().getKind() == TypeKind.VOID ||
                !method.getParameters().isEmpty()) {
            return false;
        }
        Element declaringType = method.getEnclosingElement();
        if (declaringType instanceof TypeElement) {
            for (AnnotationMirror annotationMirror : declaringType.getAnnotationMirrors()) {
                String qualifiedName = ((TypeElement)annotationMirror.getAnnotationType().asElement())
                        .getQualifiedName()
                        .toString();
                return qualifiedName.equals(IMMUTABLE_TYPE_NAME) ||
                        qualifiedName.equals(ENTITY_TYPE_NAME) ||
                        qualifiedName.equals(MAPPED_SUPERCLASS_TYPE_NAME) ||
                        qualifiedName.equals(EMBEDDABLE_TYPE_NAME);
            }
        }
        return false;
    }

    @Override
    public boolean isSetterMethod(ExecutableElement method) {
        if (super.isSetterMethod(method)) {
            TypeMirror type = method.getParameters().get(0).asType();
            if (type instanceof DeclaredType) {
                String qualifiedName = ((TypeElement)((DeclaredType) type).asElement()).getQualifiedName().toString();
                if (DRAFT_CONSUMER_TYPE_NAME.equals(qualifiedName)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}