package org.babyfish.jimmer.apt;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.MappedSuperclass;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TypeUtils {

    private final Types types;

    private final TypeMirror collectionType;

    private final TypeMirror stringType;

    private final TypeMirror numberType;

    private final TypeMirror comparableType;

    private Set<Class<? extends Annotation>> annotationTypes;

    private final Map<TypeElement, ImmutableType> immutableTypeMap = new HashMap<>();

    TypeUtils(Elements elements, Types types) {
        this.types = types;
        collectionType = types.erasure(
                elements
                        .getTypeElement(Collection.class.getName())
                        .asType()
        );
        stringType = elements
                .getTypeElement(String.class.getName())
                .asType();
        numberType = elements
                .getTypeElement(Number.class.getName())
                .asType();
        comparableType = types
                .getDeclaredType(
                        elements
                        .getTypeElement(Comparable.class.getName()),
                        types.getWildcardType(null, null)
                );
    }

    public boolean isImmutable(TypeElement typeElement) {
        return typeElement.getAnnotation(Immutable.class) != null ||
                typeElement.getAnnotation(Entity.class) != null ||
                typeElement.getAnnotation(MappedSuperclass.class) != null;
    }

    public boolean isImmutable(TypeMirror type) {
        Element element = types.asElement(type);
        return element != null && (
                element.getAnnotation(Immutable.class) != null ||
                        element.getAnnotation(Entity.class) != null ||
                        element.getAnnotation(MappedSuperclass.class) != null
        );
    }

    public boolean isMappedSuperclass(TypeMirror type) {
        Element element = types.asElement(type);
        return element != null && element.getAnnotation(MappedSuperclass.class) != null;
    }

    public boolean isEntity(TypeMirror type) {
        Element element = types.asElement(type);
        return element != null && element.getAnnotation(Entity.class) != null;
    }

    public boolean isCollection(TypeMirror type) {
        return types.isSubtype(types.erasure(type), collectionType);
    }

    public boolean isListStrictly(TypeMirror type) {
        Element element = types.asElement(type);
        return element != null && element.toString().equals("java.util.List");
    }

    public boolean isSubType(TypeMirror type, TypeMirror superType) {
        return types.isSubtype(type, superType);
    }

    public ImmutableType getImmutableType(TypeElement typeElement) {
        if (typeElement.getAnnotation(Immutable.class) != null ||
                typeElement.getAnnotation(Entity.class) != null ||
                typeElement.getAnnotation(MappedSuperclass.class) != null) {
            ImmutableType type = immutableTypeMap.get(typeElement);
            if (type == null) {
                type = new ImmutableType(this, typeElement);
                immutableTypeMap.put(typeElement, type);
            }
            return type;
        }
        return null;
    }

    public ImmutableType getImmutableType(TypeMirror type) {
        TypeElement typeElement = (TypeElement) types.asElement(type);
        return getImmutableType(typeElement);
    }

    public boolean isString(TypeMirror type) {
        return types.isSubtype(type, stringType);
    }

    public boolean isNumber(TypeMirror type) {
        return types.isSubtype(type, numberType);
    }

    public boolean isComparable(TypeMirror type) {
        return types.isSubtype(type, comparableType);
    }
}
