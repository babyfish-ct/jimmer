package org.babyfish.jimmer.apt;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.MappedSuperclass;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Context {

    @SuppressWarnings("unchecked")
    private final static Class<? extends Annotation>[] SQL_TYPE_ANNOTATION_TYPES =
            (Class<? extends Annotation>[]) new Class[] { Entity.class, MappedSuperclass.class, Embeddable.class };

    private final Elements elements;

    private final Types types;

    private final Filer filer;

    private final TypeMirror objectType;

    private final TypeMirror numberType;

    private final TypeMirror dateType;

    private final TypeMirror temporalType;

    private final TypeMirror comparableType;

    private final TypeMirror viewType;

    private final TypeMirror inputType;

    private final TypeMirror specificationType;

    private final TypeElement enumElement;

    private final Map<TypeElement, ImmutableType> immutableTypeMap = new HashMap<>();

    private final boolean keepIsPrefix;

    private final String[] includes;

    private final String[] excludes;

    private final String immutablesTypeName;

    private final String tablesTypeName;

    private final String tableExesTypeName;

    private final String fetchersTypeName;

    private final boolean hibernateValidatorEnhancement;

    private final boolean buddyIgnoreResourceGeneration;

    private final Modifier dtoFieldModifier;

    Context(
            Elements elements,
            Types types,
            Filer filer,
            boolean keepIsPrefix,
            String[] includes,
            String[] excludes,
            String immutablesTypeName,
            String tablesTypeName,
            String tableExesTypeName,
            String fetchersTypeName,
            boolean hibernateValidatorEnhancement,
            boolean buddyIgnoreResourceGeneration,
            Modifier dtoFieldModifier
    ) {
        this.elements = elements;
        this.types = types;
        this.filer = filer;
        this.keepIsPrefix = keepIsPrefix;
        this.includes = includes;
        this.excludes = excludes;
        objectType = elements
                .getTypeElement(Object.class.getName())
                .asType();
        numberType = elements
                .getTypeElement(Number.class.getName())
                .asType();
        dateType = elements
                .getTypeElement(Date.class.getName())
                .asType();
        temporalType = elements
                .getTypeElement(Temporal.class.getName())
                .asType();
        viewType = elements
                .getTypeElement(View.class.getName())
                .asType();
        inputType = elements
                .getTypeElement(Input.class.getName())
                .asType();
        specificationType = elements
                .getTypeElement(Specification.class.getName())
                .asType();
        this.immutablesTypeName = immutablesTypeName != null && !immutablesTypeName.isEmpty() ?
                immutablesTypeName :
                "Immutables";
        this.tablesTypeName = tablesTypeName != null && !tablesTypeName.isEmpty() ?
                tablesTypeName :
                "Tables";
        this.tableExesTypeName = tableExesTypeName != null && !tableExesTypeName.isEmpty() ?
                tableExesTypeName :
                "TableExes";
        this.fetchersTypeName = fetchersTypeName != null && !fetchersTypeName.isEmpty() ?
                fetchersTypeName :
                "Fetchers";
        this.hibernateValidatorEnhancement = hibernateValidatorEnhancement;
        this.buddyIgnoreResourceGeneration = buddyIgnoreResourceGeneration;
        this.dtoFieldModifier = dtoFieldModifier;
        comparableType = types
                .getDeclaredType(
                        elements
                        .getTypeElement(Comparable.class.getName()),
                        types.getWildcardType(null, null)
                );
        enumElement = elements.getTypeElement(Enum.class.getName());
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
                            typeElement,
                            "it can not be decorated by both @" +
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

    public boolean isObject(TypeMirror typeMirror) {
        return types.isAssignable(typeMirror, objectType);
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
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            TypeElement element = (TypeElement) declaredType.asElement();
            if (element.getQualifiedName().toString().equals("java.util.Collection")) {
                return true;
            }
            if (element.getSuperclass() != null && isCollection(element.getSuperclass())) {
                return true;
            }
            for (TypeMirror superItf : element.getInterfaces()) {
                if (isCollection(superItf)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isListStrictly(TypeMirror type) {
        Element element = types.asElement(type);
        return element != null && element.toString().equals("java.util.List");
    }

    public boolean isEnum(TypeMirror type) {
        return types.isSubtype(type, types.getDeclaredType(enumElement, type));
    }

    public boolean isSubType(TypeMirror type, TypeMirror superType) {
        return types.isSubtype(type, superType);
    }

    public ImmutableType getImmutableType(TypeElement typeElement) {
        ImmutableType type = immutableTypeMap.get(typeElement);
        if (type == null && !immutableTypeMap.containsKey(typeElement)) {
            if (isImmutable(typeElement)) {
                type = new ImmutableType(this, typeElement);
            }
            immutableTypeMap.put(typeElement, type);
        }
        return type;
    }

    public ImmutableType getImmutableType(TypeMirror type) {
        TypeElement typeElement = (TypeElement) types.asElement(type);
        return getImmutableType(typeElement);
    }

    public boolean isNumber(TypeMirror type) {
        return types.isSubtype(type, numberType);
    }

    public boolean isDate(TypeMirror type) {
        return types.isSubtype(type, dateType);
    }

    public boolean isTemporal(TypeMirror type) {
        return types.isSubtype(type, temporalType);
    }

    public boolean isComparable(TypeMirror type) {
        return types.isSubtype(type, comparableType);
    }

    public boolean isDto(TypeMirror type) {
        return types.isSubtype(type, viewType) ||
                types.isSubtype(type, inputType) ||
                types.isSubtype(type, specificationType);
    }

    public Elements getElements() {
        return elements;
    }

    public Types getTypes() {
        return types;
    }

    public Filer getFiler() {
        return filer;
    }

    public boolean keepIsPrefix() {
        return keepIsPrefix;
    }

    public boolean include(TypeElement typeElement) {
        if (typeElement.getAnnotation(kotlin.Metadata.class) != null) {
            return false;
        }
        String qualifiedName = typeElement.getQualifiedName().toString();
        if (includes != null) {
            for (String include : includes) {
                if (qualifiedName.startsWith(include)) {
                    return true;
                }
            }
        }
        if (excludes != null) {
            for (String exclude : excludes) {
                if (qualifiedName.startsWith(exclude)) {
                    return false;
                }
            }
        }
        return true;
    }

    public String getImmutablesTypeName() {
        return immutablesTypeName;
    }

    public String getTablesTypeName() {
        return tablesTypeName;
    }

    public String getTableExesTypeName() {
        return tableExesTypeName;
    }

    public String getFetchersTypeName() {
        return fetchersTypeName;
    }

    public boolean isHibernateValidatorEnhancement() {
        return hibernateValidatorEnhancement;
    }

    public boolean isBuddyIgnoreResourceGeneration() {
        return buddyIgnoreResourceGeneration;
    }

    public Modifier getDtoFieldModifier() {
        return dtoFieldModifier;
    }
}
