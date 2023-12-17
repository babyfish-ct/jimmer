package org.babyfish.jimmer.apt.client;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.apt.immutable.generator.Annotations;
import org.babyfish.jimmer.apt.util.GenericParser;
import org.babyfish.jimmer.client.*;
import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.client.meta.impl.*;
import org.babyfish.jimmer.error.*;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.internal.ClientException;
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
import java.util.*;

public class ClientProcessor {

    private static final String JIMMER_CLIENT = "META-INF/jimmer/client";

    private static final TypeName FETCH_BY_NAME = TypeName.of(FetchBy.class);

    private static final TypeName CODE_BASED_EXCEPTION_NAME = TypeName.of(CodeBasedException.class);

    private static final TypeName CODE_BASED_RUNTIME_EXCEPTION_NAME = TypeName.of(CodeBasedRuntimeException.class);

    private final Context context;

    private final Elements elements;

    private final Collection<String> delayedClientTypeNames;

    private final File jimmerClientFile;

    private final boolean explicitApi;

    private final SchemaBuilder<Element> builder;

    public ClientProcessor(Context context, Elements elements, Filer filer, boolean explicitApi, Collection<String> delayedClientTypeNames) {
        this.context = context;
        this.elements = elements;
        this.explicitApi = explicitApi;
        this.delayedClientTypeNames = delayedClientTypeNames;

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
            protected void throwException(Element source, String message) {
                throw new MetaException(source, message);
            }

            @Override
            protected void fillDefinition(Element source) {
                TypeElement typeElement = (TypeElement) source;
                ClientProcessor.this.fillDefinition(
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
                throw new GeneratorException("Cannot read content of  \"" + jimmerClientFile + "\"", ex);
            }
        }
        return null;
    }

    public void process(RoundEnvironment roundEnv) {

        for (Element element : roundEnv.getRootElements()) {
            handleService(element);
        }

        if (delayedClientTypeNames != null) {
            for (String delayedClientTypeName : delayedClientTypeNames) {
                handleService(context.getElements().getTypeElement(delayedClientTypeName));
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

    private void handleService(Element element) {
        if (!(element instanceof TypeElement)) {
            return;
        }
        TypeElement typeElement = (TypeElement) element;
        if (!isApiService(element)) {
            return;
        }
        if (typeElement.getNestingKind().isNested()) {
            throw new MetaException(
                    typeElement,
                    "the API service type must be top-level"
            );
        }
        if (!typeElement.getTypeParameters().isEmpty()) {
            throw new MetaException(
                    typeElement.getTypeParameters().get(0),
                    "API service cannot declare type parameters"
            );
        }
        SchemaImpl<Element> schema = builder.current();
        builder.api(typeElement, typeName(typeElement), apiService -> {
            Api api = typeElement.getAnnotation(Api.class);
            if (api != null) {
                apiService.setGroups(Arrays.asList(api.value()));
            }
            apiService.setDoc(Doc.parse(elements.getDocComment(typeElement)));
            for (Element subElement : typeElement.getEnclosedElements()) {
                if (subElement instanceof ExecutableElement && subElement.getAnnotation(ApiIgnore.class) == null) {
                    ExecutableElement executableElement = (ExecutableElement) subElement;
                    if (isApiOperation(executableElement)) {
                        handleMethod(executableElement);
                    }
                }
            }
            schema.addApiService(apiService);
        });
    }

    private void handleMethod(ExecutableElement method) {
        ApiServiceImpl<Element> service = builder.current();
        if (!method.getTypeParameters().isEmpty()) {
            throw new MetaException(
                    method.getTypeParameters().get(0),
                    "API method cannot declare type parameters"
            );
        }
        Api api = method.getAnnotation(Api.class);
        if (api == null) {
            boolean matched = false;
            if (explicitApi) {
                for (String autoOperationAnnotation : ApiOperation.AUTO_OPERATION_ANNOTATIONS) {
                    if (Annotations.annotationMirror(method, autoOperationAnnotation) != null) {
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) {
                return;
            }
        }
        builder.operation(method, method.getSimpleName().toString(), operation -> {
            if (api != null) {
                List<String> groups = Arrays.asList(api.value());
                if (groups.isEmpty()) {
                    groups = null;
                }
                List<String> parentGroups = service.getGroups();
                if (parentGroups != null && groups != null) {
                    Set<String> set = new LinkedHashSet<>(groups);
                    set.retainAll(parentGroups);
                    if (!set.isEmpty()) {
                        throw new MetaException(
                                operation.getSource(),
                                "It cannot be decorated by \"@" +
                                        Api.class +
                                        "\" with `groups` \"" +
                                        set +
                                        "\" because they are not declared in declaring type \"" +
                                        service.getTypeName() +
                                        "\""
                        );
                    }
                }
                operation.setGroups(groups);
            }
            operation.setDoc(Doc.parse(elements.getDocComment(method)));
            int[] indexRef = new int[1];
            for (VariableElement parameterElement : method.getParameters()) {
                builder.parameter(parameterElement, parameterElement.getSimpleName().toString(), parameter -> {
                    parameter.setOriginalIndex(indexRef[0]++);
                    if (Annotations.annotationMirror(parameterElement, ApiIgnore.class) != null) {
                        operation.addIgnoredParameter(parameter);
                    } else {
                        builder.typeRef(type -> {
                            fillType(parameterElement.asType());
                            parameter.setType(type);
                        });
                        operation.addParameter(parameter);
                    }
                });
            }
            if (method.getReturnType().getKind() != TypeKind.VOID) {
                builder.typeRef(type -> {
                    fillType(method.getReturnType());
                    operation.setReturnType(type);
                });
            }
            operation.setExceptionTypeNames(getExceptionTypeNames(method));
            service.addOperation(operation);
        });
    }

    private Set<TypeName> getExceptionTypeNames(ExecutableElement method) {
        List<? extends TypeMirror> exceptionTypes = method.getThrownTypes();
        if (exceptionTypes.isEmpty()) {
            return Collections.emptySet();
        }
        Set<TypeName> exceptionTypeNames = new LinkedHashSet<>();
        for (TypeMirror type : exceptionTypes) {
            TypeElement typeElement = (TypeElement) context.getTypes().asElement(type);
            collectExceptionTypeNames(typeElement, exceptionTypeNames);
        }
        return exceptionTypeNames;
    }

    private void collectExceptionTypeNames(TypeElement typeElement, Set<TypeName> exceptionTypeNames) {
        TypeName typeName = typeName(typeElement);
        if (exceptionTypeNames.contains(typeName)) {
            return;
        }
        AnnotationMirror clientException = Annotations.annotationMirror(typeElement, ClientException.class);
        if (clientException == null) {
            return;
        }
        String code = Annotations.annotationValue(clientException, "code", "");
        List<Object> subTypes = Annotations.annotationValue(clientException, "subTypes", Collections.emptyList());
        if (code.isEmpty() && subTypes.isEmpty()) {
            throw new MetaException(
                    typeElement,
                    "Illegal client exception, neither `code` nor `subTypes` of the annotation \"@" +
                            ClientException.class.getName() +
                            "\" is specified"
            );
        }
        if (!code.isEmpty() && !subTypes.isEmpty()) {
            throw new MetaException(
                    typeElement,
                    "Illegal client exception, both `code` and `subTypes` of the annotation \"@" +
                            ClientException.class.getName() +
                            "\" is specified"
            );
        }
        if (!code.isEmpty()) {
            exceptionTypeNames.add(typeName);
        } else {
            for (Object subType : subTypes) {
                String qualifiedName = subType.toString();
                if (qualifiedName.endsWith(".class")) {
                    qualifiedName = qualifiedName.substring(0, qualifiedName.length() - 6);
                }
                TypeElement subTypeElement = context.getElements().getTypeElement(qualifiedName);
                if (subTypeElement == null) {
                    throw new MetaException(
                            typeElement,
                            "Illegal client exception, the sub type \"" +
                                    qualifiedName +
                                    "\" of annotation \"@" +
                                    ClientException.class.getName() +
                                    "\" is not a class"
                    );
                }
                collectExceptionTypeNames(subTypeElement, exceptionTypeNames);
            }
        }
     }

    private void fillType(TypeMirror type) {
        if (type.getKind() != TypeKind.VOID) {
            determineTypeAndArguments(type);
            determineNullity(type);
            determineFetchBy(type);
        }
    }

    private void determineNullity(TypeMirror type) {
        TypeRefImpl<Element> typeRef = builder.current();
        Boolean forcedType = null;
        if (typeRef.getTypeName().isPrimitive()) {
            forcedType = context.getTypes().asElement(type) != null;
        }
        if (type.getAnnotation(NullableType.class) != null) {
            if (forcedType != null && !forcedType) {
                throw new MetaException(
                        builder.ancestorSource(),
                        "Illegal annotation `@NullableType` which cannot be used to decorate primitive type"
                );
            }
            typeRef.setNullable(true);
        } else {
            Element element = builder.ancestorSource(PropImpl.class, ApiParameterImpl.class, ApiOperationImpl.class);
            if (element != null) {
                for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
                    TypeElement annoElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
                    String annoClassName = annoElement.getSimpleName().toString();
                    if (annoClassName.equals("Null") || annoClassName.equals("Nullable")) {
                        if (forcedType != null && !forcedType) {
                            throw new MetaException(
                                    builder.ancestorSource(),
                                    "Illegal annotation `@" +
                                            annoElement.getQualifiedName().toString() +
                                            "` which cannot be used to decorate primitive type"
                            );
                        }
                        typeRef.setNullable(true);
                        break;
                    }
                }
            }
        }
        if (forcedType != null && forcedType) {
            typeRef.setNullable(true);
        }
    }

    private void determineFetchBy(TypeMirror entityType) {

        TypeRefImpl<Element> typeRef = builder.current();

        AnnotationMirror fetchBy = entityType.getAnnotationMirrors().stream().filter( it ->
                FETCH_BY_NAME.equals(typeName(it.getAnnotationType().asElement()))
        ).findFirst().orElse(null);
        if (fetchBy == null) {
            return;
        }
        if (!context.isImmutable(entityType)) {
            throw new MetaException(
                    builder.ancestorSource(ApiOperationImpl.class, ApiParameterImpl.class),
                    builder.ancestorSource(),
                    "Illegal type because \"" +
                            entityType +
                            "\" which is decorated by `@FetchBy` is not entity type"
            );
        }
        String constant = Annotations.annotationValue(fetchBy, "value", null);
        if (constant.isEmpty()) {
            throw new MetaException(
                    builder.ancestorSource(ApiOperationImpl.class, ApiParameterImpl.class),
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

        Element ownerElement = elements.getTypeElement(owner.toString());
        VariableElement fetcherElement = null;
        for (Element element : ownerElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.FIELD &&
                    element.getModifiers().contains(Modifier.STATIC) &&
                    element.getSimpleName().toString().equals(constant)) {
                fetcherElement = (VariableElement) element;
                break;
            }
        }
        if (fetcherElement == null) {
            throw new MetaException(
                    builder.ancestorSource(ApiOperationImpl.class, ApiParameterImpl.class),
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
                            builder.ancestorSource(ApiOperationImpl.class, ApiParameterImpl.class),
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
                    builder.ancestorSource(ApiOperationImpl.class, ApiParameterImpl.class),
                    builder.ancestorSource(),
                    "Illegal `@FetcherBy`, there is static field \"" +
                            constant +
                            "\" in owner type \"\"" +
                            owner + " but it is not fetcher for \"" +
                            ((TypeElement)((DeclaredType)entityType).asElement()).getQualifiedName() +
                            "\""
            );
        }

        typeRef.setFetchBy(constant);
        typeRef.setFetcherOwner(typeName(ownerElement));
    }

    private void determineTypeAndArguments(TypeMirror type) {

        TypeRefImpl<Element> typeRef = builder.current();

        switch (type.getKind()) {
            case BOOLEAN:
                typeRef.setTypeName(TypeName.BOOLEAN);
                break;
            case CHAR:
                typeRef.setTypeName(TypeName.CHAR);
                break;
            case BYTE:
                typeRef.setTypeName(TypeName.BYTE);
                break;
            case SHORT:
                typeRef.setTypeName(TypeName.SHORT);
                break;
            case INT:
                typeRef.setTypeName(TypeName.INT);
                break;
            case LONG:
                typeRef.setTypeName(TypeName.LONG);
                break;
            case FLOAT:
                typeRef.setTypeName(TypeName.FLOAT);
                break;
            case DOUBLE:
                typeRef.setTypeName(TypeName.DOUBLE);
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

        typeRef.setTypeName(typeName(parentElement).typeVariable(name));
    }

    private void handleWildcardType(WildcardType wildcardType) {
        TypeMirror typeMirror = wildcardType.getExtendsBound();
        if (typeMirror == null) {
            throw new UnambiguousTypeException(
                    builder.ancestorSource(ApiOperationImpl.class, ApiParameterImpl.class),
                    builder.ancestorSource(),
                    "Client API system does not accept wildcard type without extends bound"
            );
        }
        fillType(typeMirror);
    }

    private void handleIntersectionTpe(IntersectionType intersectionType) {
        fillType(intersectionType.getBounds().get(0));
    }

    private void handleArrayType(ArrayType arrayType) {
        TypeRefImpl<Element> typeRef = builder.current();
        typeRef.setTypeName(TypeName.LIST);
        builder.typeRef(argument -> {
            fillType(arrayType.getComponentType());
            typeRef.addArgument(argument);
        });
    }

    private void handleDeclaredType(DeclaredType declaredType) {

        TypeRefImpl<Element> typeRef = builder.current();

        TypeName unboxedTypeName = unboxedTypeName(declaredType);
        if (unboxedTypeName != null) {
            typeRef.setTypeName(unboxedTypeName);
            return;
        }

        TypeElement typeElement = (TypeElement) declaredType.asElement();
        if (typeElement.getNestingKind().isNested() && !typeElement.getModifiers().contains(Modifier.STATIC)) {
            throw new UnambiguousTypeException(
                    builder.ancestorSource(ApiOperationImpl.class, ApiParameterImpl.class),
                    builder.ancestorSource(),
                    "Client API service must be top-level of static nested type"
            );
        }
        TypeName typeName = typeName(typeElement);
        if (TypeName.OBJECT.equals(typeName)) {
            throw new UnambiguousTypeException(
                    builder.ancestorSource(ApiOperationImpl.class, ApiParameterImpl.class),
                    builder.ancestorSource(),
                    "Client API system does not accept unambiguous type `java.lang.Object`"
            );
        }
        switch (typeName.toString()) {
            case "java.lang.Boolean":
                typeName = TypeName.BOOLEAN;
                break;
            case "java.lang.Character":
                typeName = TypeName.CHAR;
                break;
            case "java.lang.Byte":
                typeName = TypeName.BYTE;
                break;
            case "java.lang.Short":
                typeName = TypeName.SHORT;
                break;
            case "java.lang.Integer":
                typeName = TypeName.INT;
                break;
            case "java.lang.Long":
                typeName = TypeName.LONG;
                break;
            case "java.lang.Float":
                typeName = TypeName.FLOAT;
                break;
            case "java.lang.Double":
                typeName = TypeName.DOUBLE;
                break;
        }
        typeRef.setTypeName(typeName);

        for (TypeMirror typeMirror : declaredType.getTypeArguments()) {
            builder.typeRef(argument -> {
                fillType(typeMirror);
                typeRef.addArgument(argument);
            });
        }
    }

    private void fillDefinition(TypeElement typeElement, boolean immutable) {

        if (typeElement.getKind() == ElementKind.ENUM) {
            fillEnumDefinition(typeElement);
            return;
        }

        TypeDefinitionImpl<Element> typeDefinition = builder.current();
        typeDefinition.setDoc(Doc.parse(context.getElements().getDocComment(typeElement)));
        typeDefinition.setApiIgnore(typeElement.getAnnotation(ApiIgnore.class) != null);
        if (immutable) {
            typeDefinition.setKind(TypeDefinition.Kind.IMMUTABLE);
        } else {
            typeDefinition.setKind(TypeDefinition.Kind.OBJECT);
        }

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
                    try {
                        builder.typeRef(type -> {
                            fillType(executableElement.getReturnType());
                            prop.setType(type);
                        });
                        prop.setDoc(Doc.parse(elements.getDocComment(executableElement)));
                        if (prop.getDoc() == null) {
                            Element fieldElement = typeElement.getEnclosedElements().stream().filter(
                                    it -> it.getKind() == ElementKind.FIELD &&
                                            it.getSimpleName().toString().equals(prop.getName())
                            ).findFirst().orElse(null);
                            if (fieldElement != null) {
                                prop.setDoc(Doc.parse(context.getElements().getDocComment(fieldElement)));
                            }
                        }
                        typeDefinition.addProp(prop);
                    } catch (UnambiguousTypeException ex) {
                        // Do nothing
                    }
                });
            }
        }

        ClientException clientException = typeElement.getAnnotation(ClientException.class);
        if (clientException != null && !clientException.family().isEmpty() && !clientException.code().isEmpty()) {
            typeDefinition.setError(
                    new TypeDefinition.Error(
                            clientException.family(),
                            clientException.code()
                    )
            );
        }

        if (typeElement.getKind() == ElementKind.CLASS || typeElement.getKind() == ElementKind.INTERFACE) {
            if (typeElement.getSuperclass().getKind() != TypeKind.NONE) {
                Element superElement = ((DeclaredType) typeElement.getSuperclass()).asElement();
                if (Annotations.annotationMirror(superElement, ApiIgnore.class) == null) {
                    TypeName superName = typeName(superElement);
                    if (superName.isGenerationRequired() &&
                            !superName.equals(CODE_BASED_EXCEPTION_NAME) &&
                            !superName.equals(CODE_BASED_RUNTIME_EXCEPTION_NAME)) {
                        builder.typeRef(type -> {
                            fillType(typeElement.getSuperclass());
                            typeDefinition.addSuperType(type);
                        });
                    }
                }
            }
            for (TypeMirror itf : typeElement.getInterfaces()) {
                Element superElement = ((DeclaredType) itf).asElement();
                if (Annotations.annotationMirror(superElement, ApiIgnore.class) == null) {
                    TypeName superName = typeName(superElement);
                    if (superName.isGenerationRequired() && !superName.equals(CODE_BASED_EXCEPTION_NAME)) {
                        builder.typeRef(type -> {
                            fillType(itf);
                            typeDefinition.addSuperType(type);
                        });
                    }
                }
            }
        }
    }

    private void fillEnumDefinition(TypeElement typeElement) {
        TypeDefinitionImpl<Element> definition = builder.current();
        definition.setApiIgnore(typeElement.getAnnotation(ApiIgnore.class) != null);
        definition.setKind(TypeDefinition.Kind.ENUM);
    }

    public boolean isApiService(Element element) {
        if (!(element instanceof TypeElement) || !context.include((TypeElement) element)) {
            return false;
        }
        if (element.getAnnotation(ApiIgnore.class) != null) {
            return false;
        }
        if (element.getAnnotation(Api.class) != null) {
            return true;
        }
        if (!explicitApi) {
            return false;
        }
        return Annotations.annotationMirror(
                element,
                "org.springframework.web.bind.annotation.RestController"
        ) != null;
    }

    public boolean isApiOperation(ExecutableElement element) {
        if (!element.getModifiers().contains(Modifier.PUBLIC) || element.getModifiers().contains(Modifier.STATIC)) {
            return false;
        }
        if (element.getAnnotation(ApiIgnore.class) != null) {
            return false;
        }
        if (element.getAnnotation(Api.class) != null) {
            return true;
        }
        if (!explicitApi) {
            return false;
        }
        return ApiOperation.AUTO_OPERATION_ANNOTATIONS.stream().anyMatch(it ->
            Annotations.annotationMirror(element, it) != null
        );
    }

    private static TypeName unboxedTypeName(TypeMirror type) {
        if (!(type instanceof DeclaredType)) {
            return null;
        }
        TypeElement element = (TypeElement) ((DeclaredType) type).asElement();
        switch (element.getQualifiedName().toString()) {
            case "java.lang.Boolean":
                return TypeName.BOOLEAN;
            case "java.lang.Character":
                return TypeName.CHAR;
            case "java.lang.Byte":
                return TypeName.BYTE;
            case "java.lang.Short":
                return TypeName.SHORT;
            case "java.lang.Integer":
                return TypeName.INT;
            case "java.lang.Long":
                return TypeName.LONG;
            case "java.lang.Float":
                return TypeName.FLOAT;
            case "java.lang.Double":
                return TypeName.DOUBLE;
        }
        return null;
    }

    private static TypeName typeName(Element element) {
        List<String> simpleNames = new ArrayList<>();
        String packageName = null;
        for (Element e = element; e != null; e = e.getEnclosingElement()) {
            if (e instanceof TypeElement) {
                simpleNames.add(e.getSimpleName().toString());
            } else if (e instanceof PackageElement) {
                packageName = ((PackageElement) e).getQualifiedName().toString();
            } else {
                break;
            }
        }
        Collections.reverse(simpleNames);
        return TypeName.of(packageName, simpleNames);
    }

    private static class UnambiguousTypeException extends MetaException {

        public UnambiguousTypeException(Element element, Element childElement, String reason) {
            super(element, childElement, reason);
        }
    }
}
