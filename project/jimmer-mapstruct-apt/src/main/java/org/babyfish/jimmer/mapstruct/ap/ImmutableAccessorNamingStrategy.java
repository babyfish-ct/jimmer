package org.babyfish.jimmer.mapstruct.ap;

import kotlin.Metadata;
import org.mapstruct.ap.spi.DefaultAccessorNamingStrategy;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;

public class ImmutableAccessorNamingStrategy extends DefaultAccessorNamingStrategy {

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
        if (method.getEnclosingElement().getAnnotation(Metadata.class) != null) {
            return false;
        }
        if (method.getModifiers().contains(Modifier.STATIC) ||
                method.getReturnType().getKind() == TypeKind.VOID ||
                !method.getParameters().isEmpty()) {
            return false;
        }
        return ImmutableUtils.isImmutable(method.getEnclosingElement());
    }
}