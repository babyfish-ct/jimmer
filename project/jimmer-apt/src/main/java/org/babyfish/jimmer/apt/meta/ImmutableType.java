package org.babyfish.jimmer.apt.meta;

import com.squareup.javapoet.ClassName;
import org.babyfish.jimmer.apt.TypeUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.*;
import java.util.function.Function;

public class ImmutableType {

    private String packageName;

    private String name;

    private String qualifiedName;

    private Set<Modifier> modifiers;

    private ImmutableType superType;

    private Map<String, ImmutableProp> declaredProps;

    private Map<String, ImmutableProp> props;

    private ClassName className;

    private ClassName draftClassName;

    private ClassName producerClassName;

    private ClassName implementorClassName;

    private ClassName implClassName;

    private ClassName draftImplClassName;

    public ImmutableType(
            TypeUtils typeUtils,
            TypeElement typeElement
    ) {
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

        Map<String, ImmutableProp> map = new LinkedHashMap<>();
        for (ExecutableElement executableElement : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
            ImmutableProp prop = new ImmutableProp(typeUtils, executableElement);
            map.put(prop.getName(), prop);
        }
        declaredProps = Collections.unmodifiableMap(map);

        className = toClassName(null);
        draftClassName = toClassName(name -> name + "Draft");
        producerClassName = toClassName(name -> name + "Draft", "Producer");
        implementorClassName = toClassName(name -> name + "Draft", "Producer", "Implementor");
        implClassName = toClassName(name -> name + "Draft", "Producer", "Impl");
        draftImplClassName = toClassName(name -> name + "Draft", "Producer", "DraftImpl");
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
