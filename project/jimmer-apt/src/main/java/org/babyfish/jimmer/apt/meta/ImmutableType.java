package org.babyfish.jimmer.apt.meta;

import com.squareup.javapoet.ClassName;
import org.babyfish.jimmer.apt.TypeUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ImmutableType {

    private boolean isEntity;

    private String packageName;

    private String name;

    private String qualifiedName;

    private Set<Modifier> modifiers;

    private ImmutableType superType;

    private Map<String, ImmutableProp> declaredProps;

    private Map<String, ImmutableProp> props;

    private ImmutableProp idProp;

    private ClassName className;

    private ClassName draftClassName;

    private ClassName producerClassName;

    private ClassName implementorClassName;

    private ClassName implClassName;

    private ClassName draftImplClassName;

    private ClassName tableClassName;

    private ClassName tableExClassName;

    public ImmutableType(
            TypeUtils typeUtils,
            TypeElement typeElement
    ) {
        isEntity = typeElement.getAnnotation(Entity.class) != null;
        if (typeElement.getAnnotation(Embeddable.class) != null) {
            throw new MetaException(
                    "Illegal type \"" +
                            typeElement.getQualifiedName() +
                            "\"@Embeddable is not supported"
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
        }

        if (superType != null && superType.isEntity() && !isEntity) {
            throw new MetaException(
                    "Illegal type \"" +
                            typeElement.getQualifiedName() +
                            "\", it must be entity because its super type is entity"
            );
        }

        Map<String, ImmutableProp> map = new LinkedHashMap<>();
        for (ExecutableElement executableElement : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
            ImmutableProp prop = new ImmutableProp(typeUtils, executableElement);
            map.put(prop.getName(), prop);
        }
        declaredProps = Collections.unmodifiableMap(map);
        List<ImmutableProp> idProps = declaredProps
                .values()
                .stream()
                .filter(it -> it.getAnnotation(Id.class) != null)
                .collect(Collectors.toList());
        if (superType != null) {
            if (!idProps.isEmpty()) {
                throw new MetaException(
                        "Illegal type \"" +
                                typeElement.getQualifiedName() +
                                "\", " +
                                idProps.get(0) +
                                "\" cannot be marked by @Id because id has been declared in super type"
                );
            }
            idProp = superType.idProp;
        }
        if (!isEntity) {
            if (!idProps.isEmpty()) {
                throw new MetaException(
                        "Illegal type \"" +
                                typeElement.getQualifiedName() +
                                "\", " +
                                idProps.get(0) +
                                "\" cannot be marked by @Id because current type is not entity"
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
                                "\" is marked by @Id"
                );
            }
            if (idProp == null) {
                if (idProps.isEmpty()) {
                    throw new MetaException(
                            "Illegal type \"" +
                                    typeElement.getQualifiedName() +
                                    "\", entity type must have an id property"
                    );
                }
                idProp = idProps.get(0);
            }
            if (idProp.isAssociation()) {
                throw new MetaException(
                        "Illegal property \"" +
                                idProp +
                                "\", association cannot be id property"
                );
            }
        }

        className = toClassName(null);
        draftClassName = toClassName(name -> name + "Draft");
        producerClassName = toClassName(name -> name + "Draft", "Producer");
        implementorClassName = toClassName(name -> name + "Draft", "Producer", "Implementor");
        implClassName = toClassName(name -> name + "Draft", "Producer", "Impl");
        draftImplClassName = toClassName(name -> name + "Draft", "Producer", "DraftImpl");
        tableClassName = toClassName(name -> name + "Table");
        tableExClassName = toClassName(name -> name + "Table", "Ex");
    }

    public boolean isEntity() {
        return isEntity;
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
                props = new LinkedHashMap<>(superType.getProps());
                props.putAll(declaredProps);
            }
            this.props = props;
        }
        return props;
    }

    public ImmutableProp getIdProp() {
        return idProp;
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

    public ClassName getTableExClassName() {
        return tableExClassName;
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
}
