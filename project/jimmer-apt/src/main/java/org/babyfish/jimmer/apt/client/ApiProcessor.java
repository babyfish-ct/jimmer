package org.babyfish.jimmer.apt.client;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.apt.immutable.generator.Annotations;
import org.babyfish.jimmer.apt.util.GenericParser;
import org.babyfish.jimmer.client.Api;
import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.NullableType;
import org.babyfish.jimmer.client.meta.DefaultFetcherOwner;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.Schema;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.meta.impl.*;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;

public class ApiProcessor {

    private static final String JIMMER_CLIENT = "META-INF/jimmer/client";

    private final Context context;

    private final Elements elements;

    private final Filer filer;

    private final Collection<? extends Element> delayedElements;

    private final File jimmerClientFile;

    private final SchemaBuilder<Element> builder;

    public ApiProcessor(Context context, Elements elements, Filer filer, Collection<? extends Element> delayedElements) {
        this.context = context;
        this.elements = elements;
        this.filer = filer;
        this.delayedElements = delayedElements;

        FileObject fileObject;
        try {
            fileObject = filer.getResource(StandardLocation.CLASS_OUTPUT, "", JIMMER_CLIENT);
        } catch (IOException ex) {
            throw new GeneratorException("Cannot get file object \"" + JIMMER_CLIENT + "\"", ex);
        }
        jimmerClientFile = new File(fileObject.getName());

        this.builder = new SchemaBuilder<Element>(existingSchema()) {

            @Nullable
            @Override
            protected Element loadSource(String typeName) {
                return elements.getTypeElement(typeName);
            }

            @Override
            protected RuntimeException typeNameNotFound(String typeName) {
                throw new MetaException(
                        ancestorSource(),
                        "Cannot resolve the type name \"" +
                                typeName +
                                "\""
                );
            }

            @Override
            protected void handleDefinition(Element source) {
                TypeElement typeElement = (TypeElement) source;
                ApiProcessor.this.handleDefinition(
                        typeElement,
                        typeElement.getAnnotation(Immutable.class) != null ||
                                typeElement.getAnnotation(Entity.class) != null ||
                                typeElement.getAnnotation(MappedSuperclass.class) != null ||
                                typeElement.getAnnotation(Embeddable.class) != null
                );
            }
        };
    }

    private Schema existingSchema() {
        if (jimmerClientFile.exists()) {
            try (Reader reader = new InputStreamReader(Files.newInputStream(jimmerClientFile.toPath()), StandardCharsets.UTF_8)) {
                return Schemas.readServicesFrom(reader);
            } catch (IOException ex) {
                throw new GeneratorException("Cannot read conent of  \"" + jimmerClientFile + "\"", ex);
            }
        }
        return null;
    }

    public void process(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getRootElements()) {
            process(element);
        }
        if (delayedElements != null) {
            for (Element element : delayedElements) {
                process(element);
            }
        }

        Schema schema = builder.build();
        jimmerClientFile.getParentFile().mkdirs();
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(jimmerClientFile.toPath()), StandardCharsets.UTF_8)) {
            Schemas.writeTo(schema, writer);
        } catch (IOException ex) {
            throw new GeneratorException("Cannot write \"" + jimmerClientFile + "\"", ex);
        }
    }

    private void process(Element element) {
        if (!(element instanceof TypeElement)) {
            return;
        }
        TypeElement typeElement = (TypeElement) element;
        if (!isApiService(element) || typeElement.getAnnotation(ApiIgnore.class) != null) {
            return;
        }
        if (typeElement.getNestingKind().isNested()) {
            throw new MetaException(
                    typeElement,
                    "the API interface must be top-level"
            );
        }
        if (!typeElement.getTypeParameters().isEmpty()) {
            throw new MetaException(
                    typeElement.getTypeParameters().get(0),
                    "api service cannot declare type parameters"
            );
        }
        SchemaImpl<Element> schema = builder.current();
        builder.api(typeElement, typeElement.getQualifiedName().toString(), apiService -> {
            Api api = typeElement.getAnnotation(Api.class);
            if (api != null) {
                apiService.setGroups(Arrays.asList(api.groups()));
            }
            apiService.setDoc(Doc.parse(elements.getDocComment(typeElement)));
            for (Element subElement : typeElement.getEnclosedElements()) {
                if (subElement instanceof ExecutableElement && subElement.getAnnotation(ApiIgnore.class) == null) {
                    ExecutableElement executableElement = (ExecutableElement) subElement;
                    if (executableElement.getModifiers().contains(Modifier.PUBLIC) &&
                            !executableElement.getModifiers().contains(Modifier.STATIC)) {
                        handleMethod(executableElement);
                    }
                }
            }
            schema.addApiService(apiService);
        });
    }

    private void handleMethod(ExecutableElement method) {
        ApiServiceImpl<Element> service = builder.current();
        builder.operation(method, method.getSimpleName().toString(), operation -> {
            Api api = operation.getSource().getAnnotation(Api.class);
            if (api != null) {
                if (service.getGroups() != null) {
                    throw new MetaException(
                            operation.getSource(),
                            "It cannot be decorated by \"@" +
                                    Api.class +
                                    "\" because the declaring type \"" +
                                    service.getTypeName() +
                                    "\" has been decorated by that annotation"
                    );
                }
                operation.setGroups(Arrays.asList(api.groups()));
            }
            operation.setDoc(Doc.parse(elements.getDocComment(method)));
            if (!method.getTypeParameters().isEmpty()) {
                throw new MetaException(
                        method.getTypeParameters().get(0),
                        "api method cannot declare type parameters"
                );
            }
            for (VariableElement parameterElement : method.getParameters()) {
                builder.parameter(parameterElement, parameterElement.getSimpleName().toString(), parameter -> {
                    builder.typeRef(type -> {
                        handleType(parameterElement.asType());
                        parameter.setType(type);
                    });
                    operation.addParameter(parameter);
                });
            }
            if (method.getReturnType().getKind() != TypeKind.VOID) {
                builder.typeRef(type -> {
                    handleType(method.getReturnType());
                    operation.setReturnType(type);
                });
            }
            service.addOperation(operation);
        });
    }

    private void handleType(TypeMirror type) {
        if (type.getKind() != TypeKind.VOID) {
            determineNullity(type);
            determineFetchBy(type);
            determineTypeName(type);
        }
    }

    private void determineNullity(TypeMirror type) {
        TypeRefImpl<Element> typeRef = builder.current();
        if (type.getAnnotation(NullableType.class) != null) {
            typeRef.setNullable(true);
        } else {
            Element element = builder.ancestorSource(PropImpl.class, ApiParameterImpl.class, ApiOperationImpl.class);
            if (element != null) {
                for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
                    TypeElement annoElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
                    String annoClassName = annoElement.getSimpleName().toString();
                    if (annoClassName.equals("Null") || annoClassName.equals("Nullable")) {
                        typeRef.setNullable(true);
                        break;
                    }
                }
            }
        }
    }

    private void determineFetchBy(TypeMirror entityType) {

        TypeRefImpl<Element> typeRef = builder.current();

        AnnotationMirror fetchBy = Annotations.annotationMirror(entityType, FetchBy.class);
        if (fetchBy == null) {
            return;
        }
        if (!context.isImmutable(entityType)) {
            throw new MetaException(
                    builder.ancestorSource(),
                    "Illegal type because \"" +
                            entityType +
                            "\" which is decorated by `@FetchBy` is not entity type"
            );
        }
        String constant = Annotations.annotationValue(fetchBy, "value", null);
        if (constant.isEmpty()) {
            throw new MetaException(
                    builder.ancestorSource(),
                    "The `value` of `@FetchBy` is required"
            );
        }
        Object owner = Annotations.annotationValue(fetchBy, "ownerType", null);
        if (owner == null || owner.toString().equals("void")) {
            TypeElement element = (TypeElement) builder.ancestorSource(ApiServiceImpl.class, TypeDefinitionImpl.class);
            assert element != null;
            AnnotationMirror defaultFetcherOwner = Annotations.annotationMirror(element, DefaultFetcherOwner.class);
            if (defaultFetcherOwner != null) {
                owner = Annotations.annotationValue(defaultFetcherOwner, "value", null);
            }
            if (owner == null || owner.toString().equals("void")) {
                owner = element.getQualifiedName().toString();
            }
        }

        VariableElement fetcherElement = null;
        for (Element element : elements.getTypeElement(owner.toString()).getEnclosedElements()) {
            if (element.getKind() == ElementKind.FIELD &&
                    element.getModifiers().contains(Modifier.STATIC) &&
                    element.getSimpleName().toString().equals(constant)) {
                fetcherElement = (VariableElement) element;
                break;
            }
        }
        if (fetcherElement == null) {
            throw new MetaException(
                    builder.ancestorSource(),
                    "Illegal `@FetcherBy`, there is no static field \"" +
                            constant +
                            "\" in entityType \"\"" +
                            owner
            );
        }
        TypeMirror typeMirror = fetcherElement.asType();
        String genericTypeName = null;
        if (typeMirror instanceof DeclaredType) {
            DeclaredType declaredType = (DeclaredType) typeMirror;
            TypeElement element = (TypeElement) declaredType.asElement();
            if (declaredType.getTypeArguments().isEmpty()) {
                genericTypeName = new GenericParser(
                        "fetcher",
                        element,
                        "org.babyfish.jimmer.sql.fetcher.Fetcher"
                ).parse().get(0).toString();
            } else {
                if (!element.getQualifiedName().toString().equals("org.babyfish.jimmer.sql.fetcher.Fetcher")) {
                    throw new MetaException(
                            builder.ancestorSource(),
                            "Illegal `@FetcherBy`, there is static field \"" +
                                    constant +
                                    "\" in entityType \"\"" +
                                    owner + " but it is not \"org.babyfish.jimmer.sql.fetcher.Fetcher\""
                    );
                }
                genericTypeName = declaredType.getTypeArguments().get(0).toString();
            }
        }
        if (!((TypeElement)((DeclaredType)entityType).asElement()).getQualifiedName().toString().equals(genericTypeName)) {
            throw new MetaException(
                    builder.ancestorSource(),
                    "Illegal `@FetcherBy`, there is static field \"" +
                            constant +
                            "\" in entityType \"\"" +
                            owner + " but it is not fetcher for \"" +
                            ((TypeElement)((DeclaredType)entityType).asElement()).getQualifiedName() +
                            "\""
            );
        }

        typeRef.setFetchBy(constant);
        typeRef.setFetchOwner(owner.toString());
    }

    private void determineTypeName(TypeMirror type) {

        TypeRefImpl<Element> typeRef = builder.current();

        switch (type.getKind()) {
            case BOOLEAN:
                typeRef.setTypeName("boolean");
                break;
            case CHAR:
                typeRef.setTypeName("char");
                break;
            case BYTE:
                typeRef.setTypeName("byte");
                break;
            case SHORT:
                typeRef.setTypeName("short");
                break;
            case INT:
                typeRef.setTypeName("int");
                break;
            case LONG:
                typeRef.setTypeName("long");
                break;
            case FLOAT:
                typeRef.setTypeName("float");
                break;
            case DOUBLE:
                typeRef.setTypeName("double");
                break;
            case TYPEVAR:
                handleTypeVariable((TypeVariable) type);
                break;
            case WILDCARD:
                handleWildcardType((WildcardType) type);
                break;
            case INTERSECTION:
                handleIntersectionTpe((IntersectionType) type);
                break;
            case ARRAY:
                handleArrayType((ArrayType) type);
                break;
            case DECLARED:
                handleDeclaredType((DeclaredType) type);
                break;
        }
    }

    private void handleTypeVariable(TypeVariable typeVariable) {

        TypeRefImpl<Element> typeRef = builder.current();

        Element element = typeVariable.asElement();
        TypeElement parentElement = (TypeElement) element.getEnclosingElement();
        String name = element.getSimpleName().toString();

        typeRef.setTypeName(
                "<" +
                        parentElement.getQualifiedName().toString() +
                        "::" +
                        name +
                        ">"
        );
    }

    private void handleWildcardType(WildcardType wildcardType) {
        TypeMirror typeMirror = wildcardType.getExtendsBound();
        if (typeMirror == null) {
            throw new MetaException(
                    builder.ancestorSource(),
                    "api type cannot be wildcard without extends bound"
            );
        }
        determineTypeName(typeMirror);
    }

    private void handleIntersectionTpe(IntersectionType intersectionType) {
        handleType(intersectionType.getBounds().get(0));
    }

    private void handleArrayType(ArrayType arrayType) {
        TypeRefImpl<Element> typeRef = builder.current();
        typeRef.setTypeName("java.util.List");
        builder.typeRef(argument -> {
            handleType(arrayType.getComponentType());
            typeRef.addArgument(argument);
        });
    }

    private void handleDeclaredType(DeclaredType declaredType) {

        TypeRefImpl<Element> typeRef = builder.current();

        String unboxedTypeName = unboxedTypeName(declaredType);
        if (unboxedTypeName != null) {
            typeRef.setTypeName(unboxedTypeName);
            return;
        }

        TypeElement typeElement = (TypeElement) declaredType.asElement();
        if (typeElement.getNestingKind().isNested() && !typeElement.getModifiers().contains(Modifier.STATIC)) {
            throw new MetaException(
                    builder.ancestorSource(),
                    "api type must be top-level of static nested type"
            );
        }
        String className = typeElement.getQualifiedName().toString();
        if ("java.lang.Object".equals(className)) {
            throw new MetaException(
                    builder.ancestorSource(),
                    "api type cannot be `java.lang.Object`"
            );
        }
        typeRef.setTypeName(className);

        for (TypeMirror typeMirror : declaredType.getTypeArguments()) {
            builder.typeRef(argument -> {
                handleType(typeMirror);
                typeRef.addArgument(argument);
            });
        }
    }

    private void handleDefinition(TypeElement typeElement, boolean immutable) {

        TypeDefinitionImpl<Element> typeDefinition = builder.current();
        typeDefinition.setImmutable(immutable);

        if (!immutable || typeElement.getKind() == ElementKind.INTERFACE) {
            for (Element element : typeElement.getEnclosedElements()) {
                if (!(element instanceof ExecutableElement)) {
                    continue;
                }
                ExecutableElement executableElement = (ExecutableElement) element;
                if (!executableElement.getParameters().isEmpty() ||
                        executableElement.getModifiers().contains(Modifier.STATIC) ||
                        !executableElement.getModifiers().contains(Modifier.PUBLIC) ||
                        executableElement.getReturnType().getKind() == TypeKind.VOID ||
                        executableElement.getAnnotation(ApiIgnore.class) != null) {
                    continue;
                }
                String name = executableElement.getSimpleName().toString();
                if (executableElement.getReturnType().getKind() == TypeKind.BOOLEAN &&
                        name.length() > 2 &&
                        name.startsWith("is") &&
                        !Character.isLowerCase(name.charAt(2))) {
                    name = StringUtil.identifier(name.substring(2));
                } else if (name.length() > 3 &&
                        name.startsWith("get") &&
                        !Character.isLowerCase(name.charAt(3))) {
                    name = StringUtil.identifier(name.substring(3));
                } else {
                    if (!immutable) {
                        continue;
                    }
                }
                builder.prop(executableElement, name, prop -> {
                    builder.typeRef(type -> {
                        handleType(executableElement.getReturnType());
                        prop.setType(type);
                    });
                    prop.setDoc(Doc.parse(elements.getDocComment(executableElement)));
                    typeDefinition.addProp(prop);
                });
            }
        }
        if (typeElement.getKind() == ElementKind.CLASS || typeElement.getKind() == ElementKind.INTERFACE) {
            if (typeElement.getSuperclass().getKind() != TypeKind.NONE) {
                String superName = ((TypeElement) ((DeclaredType) typeElement.getSuperclass()).asElement()).getQualifiedName().toString();
                if (TypeDefinition.isGenerationRequired(superName)) {
                    builder.typeRef(type -> {
                        handleType(typeElement.getSuperclass());
                        typeDefinition.addSuperType(type);
                    });
                }
            }
            for (TypeMirror itf : typeElement.getInterfaces()) {
                String superName = ((TypeElement)((DeclaredType) itf).asElement()).getQualifiedName().toString();
                if (TypeDefinition.isGenerationRequired(superName)) {
                    builder.typeRef(type -> {
                        handleType(itf);
                        typeDefinition.addSuperType(type);
                    });
                }
            }
        }
    }

    public static boolean isApiService(Element element) {
       return element.getAnnotation(Api.class) != null ||
               Annotations.annotationMirror(
                       element,
                       "org.springframework.web.bind.annotation.RestController"
               ) != null;
    }

    private static String unboxedTypeName(TypeMirror type) {
        if (!(type instanceof DeclaredType)) {
            return null;
        }
        TypeElement element = (TypeElement) ((DeclaredType) type).asElement();
        switch (element.getQualifiedName().toString()) {
            case "java.lang.Boolean":
                return "boolean";
            case "java.lang.Character":
                return "char";
            case "java.lang.Byte":
                return "byte";
            case "java.lang.Short":
                return "short";
            case "java.lang.Integer":
                return "int";
            case "java.lang.Long":
                return "long";
            case "java.lang.Float":
                return "float";
            case "java.lang.Double":
                return "double";
        }
        return null;
    }
}
