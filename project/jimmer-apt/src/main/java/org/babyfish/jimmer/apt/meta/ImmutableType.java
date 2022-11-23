package org.babyfish.jimmer.apt.meta;

import com.squareup.javapoet.ClassName;
import org.babyfish.jimmer.apt.TypeUtils;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.babyfish.jimmer.sql.Version;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ImmutableType {

    private final TypeElement typeElement;

    private final boolean isEntity;

    private final boolean isMappedSuperClass;

    private final String packageName;

    private final String name;

    private final String qualifiedName;

    private final Set<Modifier> modifiers;

    private final ImmutableType superType;

    private final Map<String, ImmutableProp> declaredProps;

    private Map<String, ImmutableProp> props;

    private List<ImmutableProp> propsOrderById;

    private ImmutableProp idProp;

    private ImmutableProp versionProp;

    private final ClassName className;

    private final ClassName draftClassName;

    private final ClassName producerClassName;

    private final ClassName implementorClassName;

    private final ClassName implClassName;

    private final ClassName draftImplClassName;

    private final ClassName tableClassName;

    private final ClassName tableExClassName;

    private final ClassName fetcherClassName;

    private final ClassName propsClassName;

    private final Map<ClassName, String> validationMessageMap;

    public ImmutableType(
            TypeUtils typeUtils,
            TypeElement typeElement
    ) {
        this.typeElement = typeElement;
        isEntity = typeElement.getAnnotation(Entity.class) != null;
        isMappedSuperClass = typeElement.getAnnotation(MappedSuperclass.class) != null;
        if (isEntity && isMappedSuperClass) {
            throw new MetaException(
                    "Illegal type \"" +
                            typeElement.getQualifiedName() +
                            "\", it cannot be decorated by both @Entity and @isMappedSuperClass"
            );
        }

        packageName = ((PackageElement)typeElement.getEnclosingElement()).getQualifiedName().toString();
        name = typeElement.getSimpleName().toString();
        qualifiedName = typeElement.getQualifiedName().toString();
        modifiers = typeElement.getModifiers();

        TypeMirror superTypeMirror = null;
        for (TypeMirror itf : typeElement.getInterfaces()) {
            if (typeUtils.isImmutable(itf)) {
                if (superTypeMirror != null) {
                    throw new MetaException(
                            String.format(
                                    "'%s' inherits multiple Immutable interfaces",
                                    typeElement.getQualifiedName()
                            )
                    );
                }
                superTypeMirror = itf;
            }
        }
        if (superTypeMirror != null) {
            superType = typeUtils.getImmutableType(superTypeMirror);
        } else {
            superType = null;
        }

        if (superType != null) {
            if (this.isEntity || this.isMappedSuperClass) {
                if (superType.isEntity()) {
                    throw new MetaException(
                            "Illegal type \"" +
                                    typeElement.getQualifiedName() +
                                    "\", it super type \"" +
                                    superType.qualifiedName +
                                    "\" is entity. " +
                                    "Super entity is not supported temporarily, " +
                                    "please use an interface decorated by @MappedSuperClass to be the super type"
                    );
                }
                if (!superType.isMappedSuperClass) {
                    throw new MetaException(
                            "Illegal type \"" +
                                    typeElement.getQualifiedName() +
                                    "\", it super type \"" +
                                    superType.qualifiedName +
                                    "\" is entity is not decorated by @MappedSuperClass"
                    );
                }
            } else if (superType.isEntity || superType.isMappedSuperClass) {
                throw new MetaException(
                        "Illegal type \"" +
                                typeElement.getQualifiedName() +
                                "\", it super type \"" +
                                superType.qualifiedName +
                                "\" cannot be decorated by @Entity or @MappedSuperClass"
                );
            }
        }

        int propIdSequence = superType != null ? superType.getProps().size() : 0;
        Map<String, ImmutableProp> map = new LinkedHashMap<>();
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(typeElement.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            if (executableElement.isDefault()) {
                for (AnnotationMirror am : executableElement.getAnnotationMirrors()) {
                    if (((TypeElement)am.getAnnotationType().asElement()).getQualifiedName().toString().startsWith("org.babyfish.jimmer.")) {
                        throw new MetaException(
                                "Illegal method \"" + executableElement + "\", it " +
                                        "is default method so that it cannot be decorated by any jimmer annotations"
                        );
                    }
                }
            }
        }
        for (ExecutableElement executableElement : executableElements) {
            if (!executableElement.isDefault() && executableElement.getAnnotation(Id.class) != null) {
                ImmutableProp prop = new ImmutableProp(typeUtils, executableElement, ++propIdSequence);
                map.put(prop.getName(), prop);
            }
        }
        for (ExecutableElement executableElement : executableElements) {
            if (!executableElement.isDefault() && executableElement.getAnnotation(Id.class) == null) {
                ImmutableProp prop = new ImmutableProp(typeUtils, executableElement, ++propIdSequence);
                map.put(prop.getName(), prop);
            }
        }
        if (superType != null) {
            for (Map.Entry<String, ImmutableProp> e : map.entrySet()) {
                if (superType.getProps().containsKey(e.getKey())) {
                    throw new ModelException(
                            "The property \"" +
                                    e.getValue() +
                                    "\" overrides property of super type, this is not allowed"
                    );
                }
            }
        }
        declaredProps = Collections.unmodifiableMap(map);
        List<ImmutableProp> idProps = declaredProps
                .values()
                .stream()
                .filter(it -> it.getAnnotation(Id.class) != null)
                .collect(Collectors.toList());
        List<ImmutableProp> versionProps = declaredProps
                .values()
                .stream()
                .filter(it -> it.getAnnotation(Version.class) != null)
                .collect(Collectors.toList());
        if (superType != null) {
            if (superType.getIdProp() != null && !idProps.isEmpty()) {
                throw new MetaException(
                        "Illegal type \"" +
                                typeElement.getQualifiedName() +
                                "\", " +
                                idProps.get(0) +
                                "\" cannot be decorated by @Id because id has been declared in super type"
                );
            }
            if (superType.getVersionProp() != null && !versionProps.isEmpty()) {
                throw new MetaException(
                        "Illegal type \"" +
                                typeElement.getQualifiedName() +
                                "\", " +
                                versionProps.get(0) +
                                "\" cannot be decorated by @Version because version has been declared in super type"
                );
            }
            idProp = superType.idProp;
            versionProp = superType.versionProp;
        }
        if (!isEntity && !isMappedSuperClass) {
            if (!idProps.isEmpty()) {
                throw new MetaException(
                        "Illegal type \"" +
                                typeElement.getQualifiedName() +
                                "\", " +
                                idProps.get(0) +
                                "\" cannot be decorated by @Id because current type is not entity"
                );
            }
            if (!versionProps.isEmpty()) {
                throw new MetaException(
                        "Illegal type \"" +
                                typeElement.getQualifiedName() +
                                "\", " +
                                versionProps.get(0) +
                                "\" cannot be decorated by @Version because current type is not entity"
                );
            }
        } else {
            if (idProps.size() > 1) {
                throw new MetaException(
                        "Illegal type \"" +
                                typeElement.getQualifiedName() +
                                "\", multiple id properties are not supported, " +
                                "but both \"" +
                                idProps.get(0) +
                                "\" and \"" +
                                idProps.get(1) +
                                "\" is decorated by @Id"
                );
            }
            if (versionProps.size() > 1) {
                throw new MetaException(
                        "Illegal type \"" +
                                typeElement.getQualifiedName() +
                                "\", multiple id properties are not supported, " +
                                "but both \"" +
                                versionProps.get(0) +
                                "\" and \"" +
                                versionProps.get(1) +
                                "\" is decorated by @Version"
                );
            }
            if (idProp == null) {
                if (isEntity && idProps.isEmpty()) {
                    throw new MetaException(
                            "Illegal type \"" +
                                    typeElement.getQualifiedName() +
                                    "\", entity type must have an id property"
                    );
                }
                if (!idProps.isEmpty()) {
                    idProp = idProps.get(0);
                }
            }
            if (idProp != null && idProp.isAssociation()) {
                throw new MetaException(
                        "Illegal property \"" +
                                idProp +
                                "\", association cannot be id property"
                );
            }
            if (versionProp == null && !versionProps.isEmpty()) {
                versionProp = versionProps.get(0);
                if (versionProp.isAssociation()) {
                    throw new MetaException(
                            "Illegal property \"" +
                                    versionProps +
                                    "\", association cannot be version property"
                    );
                }
            }
        }

        className = toClassName(null);
        draftClassName = toClassName(name -> name + "Draft");
        producerClassName = toClassName(name -> name + "Draft", "Producer");
        implementorClassName = toClassName(name -> name + "Draft", "Producer", "Implementor");
        implClassName = toClassName(name -> name + "Draft", "Producer", "Impl");
        draftImplClassName = toClassName(name -> name + "Draft", "Producer", "DraftImpl");
        tableClassName = toClassName(name -> name + "Table");
        tableExClassName = toClassName(name -> name + "TableEx");
        fetcherClassName = toClassName(name -> name + "Fetcher");
        propsClassName = toClassName(name -> name + "Props");

        validationMessageMap = ValidationMessages.parseMessageMap(typeElement);
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public boolean isEntity() {
        return isEntity;
    }

    public boolean isMappedSuperClass() {
        return isMappedSuperClass;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public ImmutableType getSuperType() {
        return superType;
    }

    public Map<String, ImmutableProp> getDeclaredProps() {
        return declaredProps;
    }

    public Map<String, ImmutableProp> getProps() {
        Map<String, ImmutableProp> props = this.props;
        if (props == null) {
            if (superType == null) {
                props = declaredProps;
            } else {
                props = new LinkedHashMap<>();
                for (ImmutableProp prop : superType.getProps().values()) {
                    if (prop.getAnnotation(Id.class) != null) {
                        props.put(prop.getName(), prop);
                    }
                }
                for (ImmutableProp prop : declaredProps.values()) {
                    if (prop.getAnnotation(Id.class) != null) {
                        props.put(prop.getName(), prop);
                    }
                }
                for (ImmutableProp prop : superType.getProps().values()) {
                    if (prop.getAnnotation(Id.class) == null) {
                        props.put(prop.getName(), prop);
                    }
                }
                for (ImmutableProp prop : declaredProps.values()) {
                    if (prop.getAnnotation(Id.class) == null) {
                        props.put(prop.getName(), prop);
                    }
                }
            }
            this.props = props;
        }
        return props;
    }

    public List<ImmutableProp> getPropsOrderById() {
        List<ImmutableProp> list = propsOrderById;
        if (list == null) {
            this.propsOrderById = list = getProps()
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(ImmutableProp::getId))
                    .collect(Collectors.toList());
        }
        return list;
    }

    public ImmutableProp getIdProp() {
        return idProp;
    }

    public ImmutableProp getVersionProp() {
        return versionProp;
    }

    public ClassName getClassName() {
        return className;
    }

    public ClassName getDraftClassName() {
        return draftClassName;
    }

    public ClassName getProducerClassName() {
        return producerClassName;
    }

    public ClassName getImplementorClassName() {
        return implementorClassName;
    }

    public ClassName getImplClassName() {
        return implClassName;
    }

    public ClassName getDraftImplClassName() {
        return draftImplClassName;
    }

    public ClassName getTableClassName() {
        return tableClassName;
    }

    public ClassName getNewTableClassName() {
        return toClassName(name -> name + "s");
    }

    public ClassName getTableExClassName() {
        return tableExClassName;
    }

    public ClassName getFetcherClassName() {
        return fetcherClassName;
    }

    public ClassName getPropsClassName() {
        return propsClassName;
    }

    private ClassName toClassName(
            Function<String, String> transform,
            String ... moreSimpleNames
    ) {
        return ClassName.get(
                packageName,
                transform != null ? transform.apply(name) : name,
                moreSimpleNames
        );
    }

    public Map<ClassName, String> getValidationMessageMap() {
        return validationMessageMap;
    }
}
