package org.babyfish.jimmer.apt;

import com.squareup.javapoet.ClassName;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.apt.meta.ImmutableType;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.persistence.Entity;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class TypeUtils {

    private Types types;

    private TypeMirror collectionType;

    private TypeMirror stringType;

    private TypeMirror numberType;

    private TypeMirror comparableType;

    private Set<Class<? extends Annotation>> annotationTypes;

    private Map<TypeElement, ImmutableType> immutableTypeMap = new HashMap<>();

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
        comparableType = elements
                .getTypeElement(Comparable.class.getName())
                .asType();
    }

    public boolean isImmutable(TypeElement typeElement) {
        return typeElement.getAnnotation(Immutable.class) != null ||
                typeElement.getAnnotation(Entity.class) != null;
    }

    public boolean isImmutable(TypeMirror type) {
        Element element = types.asElement(type);
        return element != null && (
                element.getAnnotation(Immutable.class) != null ||
                        element.getAnnotation(Entity.class) != null
        );
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

    public ImmutableType getImmutableType(TypeElement typeElement) {
        if (typeElement.getAnnotation(Immutable.class) != null || typeElement.getAnnotation(Entity.class) != null) {
            return immutableTypeMap.computeIfAbsent(typeElement, it -> new ImmutableType(this, it));
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
