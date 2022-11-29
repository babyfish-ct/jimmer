package org.babyfish.jimmer.apt;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.apt.meta.MetaException;
import org.babyfish.jimmer.sql.Embeddable;
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

    @SuppressWarnings("unchecked")
    private final static Class<? extends Annotation>[] SQL_TYPE_ANNOTATION_TYPES =
            (Class<? extends Annotation>[]) new Class[] { Entity.class, MappedSuperclass.class, Embeddable.class };

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

    public Class<? extends Annotation> getImmutableAnnotationType(TypeElement typeElement) {
        if (typeElement == null) {
            return null;
        }
        Annotation annotation = typeElement.getAnnotation(Immutable.class);
        Annotation sqlAnnotation = null;
        for (Class<? extends Annotation> sqlAnnotationType : SQL_TYPE_ANNOTATION_TYPES) {
            Annotation newSqlAnnotation = typeElement.getAnnotation(sqlAnnotationType);
            if (newSqlAnnotation != null) {
                if (sqlAnnotation != null) {
                    throw new MetaException(
                            "Illegal type \"" +
                                    typeElement.getQualifiedName().toString() +
                                    "\", it can not be decorated by both @" +
                                    sqlAnnotation.annotationType().getName() +
                                    " and @" +
                                    newSqlAnnotation.annotationType().getName()
                    );
                }
                sqlAnnotation = newSqlAnnotation;
            }
        }
        if (sqlAnnotation != null) {
            return sqlAnnotation.annotationType();
        }
        if (annotation != null) {
            return annotation.annotationType();
        }
        return null;
    }

    public Class<? extends Annotation> getImmutableAnnotationType(TypeMirror typeMirror) {
        Element element = types.asElement(typeMirror);
        return getImmutableAnnotationType((TypeElement) element);
    }

    public boolean isImmutable(TypeElement type) {
        return getImmutableAnnotationType(type) != null;
    }

    public boolean isImmutable(TypeMirror type) {
        return getImmutableAnnotationType(type) != null;
    }

    public boolean isEntity(TypeMirror type) {
        return getImmutableAnnotationType(type) == Entity.class;
    }

    public boolean isMappedSuperclass(TypeMirror type) {
        return getImmutableAnnotationType(type) == MappedSuperclass.class;
    }

    public boolean isEmbeddable(TypeMirror type) {
        return getImmutableAnnotationType(type) == Embeddable.class;
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
        if (getImmutableAnnotationType(typeElement) != null) {
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
