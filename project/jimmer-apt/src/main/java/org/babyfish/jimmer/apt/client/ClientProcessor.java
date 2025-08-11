package org.babyfish.jimmer.apt.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import org.babyfish.jimmer.ClientException;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.apt.immutable.generator.Annotations;
import org.babyfish.jimmer.apt.immutable.generator.Constants;
import org.babyfish.jimmer.apt.util.ConverterMetadata;
import org.babyfish.jimmer.apt.util.GenericParser;
import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.TNullable;
import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.client.meta.impl.*;
import org.babyfish.jimmer.error.CodeBasedException;
import org.babyfish.jimmer.error.CodeBasedRuntimeException;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

public class ClientProcessor {

    private static final String JIMMER_CLIENT = "META-INF/jimmer/client";

    private static final Method RECORD_COMPONENT_ELEMENT_GET_ACCESSOR;

    private static final TypeName FETCH_BY_NAME = TypeName.of(FetchBy.class);

    private static final TypeName CODE_BASED_EXCEPTION_NAME = TypeName.of(CodeBasedException.class);

    private static final TypeName CODE_BASED_RUNTIME_EXCEPTION_NAME = TypeName.of(CodeBasedRuntimeException.class);

    private final Context context;

    private final DocMetadata docMetadata;

    private final ClientExceptionContext clientExceptionContext;

    private final Collection<String> delayedClientTypeNames;

    private final File jimmerClientFile;

    private final boolean explicitApi;

    private final SchemaBuilder<Element> builder;

    private final Set<TypeName> jsonValueTypeNameStack = new HashSet<>();

    public ClientProcessor(
            Context context,
            boolean explicitApi,
            Collection<String> delayedClientTypeNames
    ) {
        this.context = context;
        this.clientExceptionContext = new ClientExceptionContext(context);
        this.docMetadata = new DocMetadata(context);
        this.explicitApi = explicitApi;
        this.delayedClientTypeNames = delayedClientTypeNames;

        FileObject fileObject;
        try {
            fileObject = context.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", JIMMER_CLIENT);
        } catch (IOException ex) {
            throw new GeneratorException("Cannot get file object \"" + JIMMER_CLIENT + "\"", ex);
        }
        jimmerClientFile = new File(fileObject.getName());

        this.builder = new SchemaBuilder<Element>(existingSchema()) {

            @Nullable
            @Override
            protected Element loadSource(String typeName) {
                return context.getElements().getTypeElement(typeName);
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

        checkJdkVersion(roundEnv);

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

    /**
     * Find this problem on `zulu-1.8 jdk`,
     * `TypeMirror.getAnnotationMirrors` always returns empty list if
     * the current `TypeMirror` is not top type but generic argument.
     */
    private void checkJdkVersion(RoundEnvironment roundEnv) {
        try {
            String.class.getMethod("isBlank");
            return;
        } catch (NoSuchMethodException e) {
            // Do nothing
        }
        boolean hasApiService = false;
        for (Element element : roundEnv.getRootElements()) {
            if (isApiService(element)) {
                hasApiService = true;
                break;
            }
        }
        if (!hasApiService && delayedClientTypeNames != null) {
            for (String typeName : delayedClientTypeNames) {
                if (isApiService(context.getElements().getTypeElement(typeName))) {
                    hasApiService = true;
                    break;
                }
            }
        }
        if (hasApiService) {
            throw new FetchByUnsupportedException();
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
            apiService.setDoc(docMetadata.getDoc(typeElement));
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
            operation.setDoc(docMetadata.getDoc(method));
            int[] indexRef = new int[1];
            for (VariableElement parameterElement : method.getParameters()) {
                builder.parameter(parameterElement, parameterElement.getSimpleName().toString(), parameter -> {
                    parameter.setOriginalIndex(indexRef[0]++);
                    if (Annotations.annotationMirror(parameterElement, ApiIgnore.class) != null) {
                        operation.addIgnoredParameter(parameter);
                    } else {
                        builder.typeRef(type -> {
                            fillType(parameterElement.asType());
                            setNullityByJetBrainsAnnotation(type, parameterElement, parameterElement.asType());
                            parameter.setType(type);
                        });
                        operation.addParameter(parameter);
                    }
                });
            }
            if (method.getReturnType().getKind() != TypeKind.VOID) {
                builder.typeRef(type -> {
                    fillType(method.getReturnType());
                    setNullityByJetBrainsAnnotation(type, method, method.getReturnType());
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
            if (typeElement.getAnnotation(ClientException.class) != null) {
                collectExceptionTypeNames(clientExceptionContext.get(typeElement), exceptionTypeNames);
            }
        }
        return exceptionTypeNames;
    }

    private void collectExceptionTypeNames(ClientExceptionMetadata metadata, Set<TypeName> exceptionTypeNames) {
        if (metadata.getCode() != null) {
            exceptionTypeNames.add(typeName(metadata.getElement()));
        }
        for (ClientExceptionMetadata subMetadata : metadata.getSubMetdatas()) {
            collectExceptionTypeNames(subMetadata, exceptionTypeNames);
        }
     }

    private void fillType(TypeMirror type) {
        if (type.getKind() != TypeKind.VOID) {
            TypeRefImpl<Element> typeRef = builder.current();
            try {
                determineTypeAndArguments(type);
                determineNullity(type);
                determineFetchBy(type);
                removeOptional(typeRef);
            } catch (JsonValueTypeChangeException ex) {
                typeRef.replaceBy(ex.typeRef, typeRef.isNullable() || ex.typeRef.isNullable());
            }
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
        if (!context.isEntity(entityType)) {
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

        Element ownerElement = context.getElements().getTypeElement(owner.toString());
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
                ).parse().argumentTypeNames.get(0).toString();
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
        typeRef.setFetcherDoc(docMetadata.getDoc(fetcherElement));
    }

    private void determineNullity(TypeMirror type) {
        TypeRefImpl<Element> typeRef = builder.current();
        boolean isRawTypePrimitive = type.getKind().isPrimitive();
        boolean isPrimitive = typeRef.getTypeName().isPrimitive();
        if (type.getAnnotation(TNullable.class) != null) {
            if (isRawTypePrimitive) {
                throw new MetaException(
                        builder.ancestorSource(),
                        "Illegal annotation `@" +
                                TNullable.class.getName() +
                                "` which cannot be used to decorate primitive type"
                );
            }
            typeRef.setNullable(true);
        }
        if (isPrimitive && !isRawTypePrimitive) {
            TypeRef parentRef = builder.parent(TypeRefImpl.class);
            if (parentRef == null) {
                typeRef.setNullable(true);
            }
        }
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
                handleIntersectionType((IntersectionType) type);
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

    private void handleIntersectionType(IntersectionType intersectionType) {
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
                    "Client API only accept top-level of static nested type"
            );
        }
        TypeName typeName = typeName(typeElement);
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
            case "java.lang.Object":
                typeName = TypeName.OBJECT;
                break;
        }

        TypeRefImpl<Element> jsonValueTypeRef = jsonValueTypeRef(typeName);
        if (jsonValueTypeRef != null) {
            throw new JsonValueTypeChangeException(jsonValueTypeRef);
        }
        String simpleName = typeElement.getSimpleName().toString();

        // 对于JsonObject等特殊类型，将其视为Object类型
        boolean jsonFlag = Stream.of(
                "JsonNode",
                "JSONObject",
                "JsonObject",
                "JsonElement",
                "ObjectNode",
                "ArrayNode"
        ).anyMatch(simpleName::equalsIgnoreCase);
        if (jsonFlag) {
            typeRef.setTypeName(TypeName.OBJECT);
            return;
        }




        if (!typeElement.getTypeParameters().isEmpty() && declaredType.getTypeArguments().isEmpty()) {
            throw new NoGenericArgumentsException(
                    builder.ancestorSource(ApiOperationImpl.class, ApiParameterImpl.class),
                    builder.ancestorSource(),
                    "Client API system does not accept raw type of generic type"
            );
        }

        typeRef.setTypeName(typeName);
        for (TypeMirror typeMirror : declaredType.getTypeArguments()) {
            builder.typeRef(argument -> {
                fillType(typeMirror);
                typeRef.addArgument(argument);
            });
        }
    }

    @SuppressWarnings("unchecked")
    private TypeRefImpl<Element> jsonValueTypeRef(TypeName typeName) {
        TypeElement typeElement = context.getElements().getTypeElement(typeName.toString());
        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getAnnotation(JsonValue.class) == null) {
                continue;
            }
            if (element.getKind() != ElementKind.METHOD || element.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            ExecutableElement methodElement = (ExecutableElement) element;
            if (!methodElement.getParameters().isEmpty() || methodElement.getReturnType().getKind() == TypeKind.VOID) {
                continue;
            }
            if (!jsonValueTypeNameStack.add(typeName)) {
                throw new MetaException(
                        builder.ancestorSource(ApiOperationImpl.class, ApiParameterImpl.class),
                        builder.ancestorSource(),
                        "Cannot resolve \"@" +
                                JsonValue.class.getName() +
                                "\" because of dead recursion: " +
                                jsonValueTypeNameStack
                );
            }
            try {
                TypeRefImpl<Element>[] jsonValueTypRef = (TypeRefImpl<Element>[]) new TypeRefImpl[1];
                builder.typeRef(type -> {
                    fillType(methodElement.getReturnType());
                    jsonValueTypRef[0] = type;
                });
                return jsonValueTypRef[0];
            } finally {
                jsonValueTypeNameStack.remove(typeName);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void fillDefinition(TypeElement typeElement, boolean immutable) {

        TypeDefinitionImpl<Element> typeDefinition = builder.current();
        typeDefinition.setDoc(docMetadata.getDoc(typeElement));

        if (typeElement.getKind() == ElementKind.ENUM) {
            fillEnumDefinition(typeElement);
            return;
        }

        typeDefinition.setApiIgnore(typeElement.getAnnotation(ApiIgnore.class) != null);
        if (immutable) {
            typeDefinition.setKind(TypeDefinition.Kind.IMMUTABLE);
        } else {
            typeDefinition.setKind(TypeDefinition.Kind.OBJECT);
        }

        if (!immutable || typeElement.getKind() == ElementKind.INTERFACE) {
            boolean isClientException = typeElement.getAnnotation(ClientException.class) != null;
            for (Element element : typeElement.getEnclosedElements()) {
                if (element.getKind().name().equals("RECORD_COMPONENT")) {
                    if (RECORD_COMPONENT_ELEMENT_GET_ACCESSOR == null) {
                        continue;
                    }
                    try {
                        element = (Element) RECORD_COMPONENT_ELEMENT_GET_ACCESSOR.invoke(element);
                    } catch (IllegalAccessException e) {
                        throw new AssertionError(e);
                    } catch (InvocationTargetException e) {
                        throw new AssertionError(e.getTargetException());
                    }
                }
                if (!(element instanceof ExecutableElement)) {
                    continue;
                }

                ExecutableElement executableElement = (ExecutableElement) element;
                if (!executableElement.getParameters().isEmpty() ||
                        executableElement.getModifiers().contains(Modifier.STATIC) ||
                        !executableElement.getModifiers().contains(Modifier.PUBLIC) ||
                        executableElement.getReturnType().getKind() == TypeKind.VOID ||
                        executableElement.getAnnotation(ApiIgnore.class) != null ||
                        executableElement.getAnnotation(JsonIgnore.class) != null
                ) {
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
                    if (!immutable && !typeElement.getKind().name().equals("RECORD")) {
                        continue;
                    }
                }
                if (Arrays.asList("toString", "hashCode").stream().anyMatch(name::equals)) {
                    continue;
                }
                if (isClientException && (name.equals("code") || name.equals("fields"))) {
                    continue;
                }
                ConverterMetadata metadata;
                if (immutable) {
                    metadata = context
                            .getImmutableType(typeElement)
                            .getProps()
                            .get(name)
                            .getConverterMetadata();
                } else {
                    metadata = null;
                }
                builder.prop(executableElement, name, prop -> {
                    try {
                        builder.typeRef(type -> {
                            fillType(metadata != null ? metadata.getTargetType() : executableElement.getReturnType());
                            setNullityByJetBrainsAnnotation(type, executableElement, executableElement.getReturnType());
                            prop.setType(type);
                        });
                        prop.setDoc(docMetadata.getDoc(executableElement));
                        typeDefinition.addProp(prop);
                    } catch (UnambiguousTypeException ex) {
                        // Do nothing
                    }
                });
            }
            for (Element fieldElement : typeElement.getEnclosedElements()) {
                if (fieldElement.getKind() != ElementKind.FIELD || fieldElement.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }
                PropImpl<Element> prop = (PropImpl<Element>) typeDefinition.getPropMap().get(fieldElement.getSimpleName().toString());
                if (prop == null) {
                    continue;
                }
                if (fieldElement.getAnnotation(JsonIgnore.class) != null) {
                    typeDefinition.getPropMap().remove(fieldElement.getSimpleName().toString());
                }
                if (prop.getDoc() == null) {
                    prop.setDoc(docMetadata.getDoc(fieldElement));
                }
            }
        }

        ClientException clientException = typeElement.getAnnotation(ClientException.class);
        if (clientException != null) {
            ClientExceptionMetadata metadata = clientExceptionContext.get(typeElement);
            if (metadata.getCode() != null && !metadata.getCode().isEmpty()) {
                typeDefinition.setError(
                        new TypeDefinition.Error(
                                metadata.getFamily(),
                                metadata.getCode()
                        )
                );
            }
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

        for (Element constantElement : typeElement.getEnclosedElements()) {
            if (constantElement.getKind() != ElementKind.ENUM_CONSTANT) {
                continue;
            }
            builder.constant(constantElement, constantElement.getSimpleName().toString(), constant -> {
                constant.setDoc(docMetadata.getDoc(constantElement));
                definition.addEnumConstant(constant);
            });
        }
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

    private void setNullityByJetBrainsAnnotation(TypeRefImpl<Element> typeRef, Element element, TypeMirror rawType) {
        if (typeRef.isNullable()) {
            return;
        }
        boolean isPrimitive = typeRef.getTypeName().isPrimitive();
        boolean isRawTypePrimitive = rawType.getKind().isPrimitive();
        String nullableTypeName = null;
        String nonNullTypeName = null;
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            TypeElement annoElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
            String annoClassName = annoElement.getSimpleName().toString();
            String annoClassFullName = annoElement.getQualifiedName().toString();
            if (annoClassName.equals("Null") ||
                    annoClassName.equals("Nullable") ||
                    annoElement.getQualifiedName().toString().equals(Constants.T_NULLABLE_QUALIFIED_NAME)) {
                if (isRawTypePrimitive) {
                    throw new MetaException(
                            builder.ancestorSource(),
                            "Illegal annotation `@" +
                                    annoClassName +
                                    "` which cannot be used to decorate primitive type"
                    );
                }
                nullableTypeName = annoClassFullName;
            } else if ((annoClassName.equals("NotNull") || annoClassName.equals("NonNull")) &&
                    !annoClassFullName.equals("javax.validation.constraints.NotNull") &&
                    !annoClassFullName.equals("jakarta.validation.constraints.NotNull")) {
                if (isPrimitive && !isRawTypePrimitive) {
                    throw new MetaException(
                            builder.ancestorSource(),
                            "Illegal annotation `@" +
                                    annoClassName +
                                    "` which cannot be used to decorate boxed type of primitive type, " +
                                    "please replace it to unboxed primitive type"
                    );
                }
                nonNullTypeName = annoClassFullName;
            }
        }
        if (nullableTypeName != null && nonNullTypeName != null) {
            throw new MetaException(
                    element,
                    "Conflict nullity annotation \"@" +
                            nullableTypeName +
                            "\" and \"@" +
                            nonNullTypeName +
                            "\""
            );
        }
        if (nullableTypeName != null) {
            typeRef.setNullable(true);
        }
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

    @SuppressWarnings("unchecked")
    private static void removeOptional(TypeRefImpl<Element> typeRef) {
        if (typeRef.getTypeName().equals(TypeName.OPTIONAL)) {
            TypeRefImpl<Element> target = (TypeRefImpl<Element>) typeRef.getArguments().get(0);
            typeRef.replaceBy(target, true);
        }
    }

    private static class UnambiguousTypeException extends MetaException {

        public UnambiguousTypeException(Element element, Element childElement, String reason) {
            super(element, childElement, reason);
        }
    }

    private static class NoGenericArgumentsException extends MetaException {

        public NoGenericArgumentsException(Element element, Element childElement, String reason) {
            super(element, childElement, reason);
        }
    }

    private static class JsonValueTypeChangeException extends RuntimeException {

        final TypeRefImpl<Element> typeRef;

        private JsonValueTypeChangeException(TypeRefImpl<Element> typeRef) {
            this.typeRef = typeRef;
        }
    }

    static {
        Method method = null;
        if (Arrays.stream(ElementKind.values()).anyMatch(it -> it.name().equals("RECORD_COMPONENT"))) {
            try {
                Class rce = Class.forName("javax.lang.model.element.RecordComponentElement");
                method = rce.getMethod("getAccessor");
            } catch (ClassNotFoundException | NoSuchMethodException ex) {
                // Do nothing
            }
        }
        RECORD_COMPONENT_ELEMENT_GET_ACCESSOR = method;
    }
}
