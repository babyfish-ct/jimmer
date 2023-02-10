package org.babyfish.jimmer.mapstruct.ap;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.MappedSuperclass;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

class ImmutableUtils {

    private ImmutableUtils() {}

    public static boolean isImmutable(Element element) {
        if (element.getKind() != ElementKind.INTERFACE) {
            return false;
        }
        TypeElement typeElement = (TypeElement) element;
        return typeElement.getAnnotation(Immutable.class) != null ||
                typeElement.getAnnotation(Entity.class) != null ||
                typeElement.getAnnotation(MappedSuperclass.class) != null ||
                typeElement.getAnnotation(Embeddable.class) != null;
    }
}
