package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.client.meta.impl.ApiServiceImpl;
import org.babyfish.jimmer.client.meta.impl.SchemaImpl;
import org.babyfish.jimmer.client.meta.impl.Schemas;
import org.babyfish.jimmer.client.meta.impl.TypeDefinitionImpl;
import org.babyfish.jimmer.client.runtime.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class MetadataBuilder implements Metadata.Builder {

    private Metadata.OperationParser operationParser;

    private Metadata.ParameterParser parameterParser;

    private Set<String> groups;

    private boolean genericSupported;

    private String uriPrefix;

    private Map<TypeName, VirtualType> virtualTypeMap = Collections.emptyMap();

    private Set<Class<?>> ignoredParameterTypes = new HashSet<>();

    private Set<Class<?>> illegalReturnTypes = new HashSet<>();

    private Set<String> ignoredParameterTypeNames = new HashSet<>();

    private Set<String> illegalReturnTypeNames = new HashSet<>();

    public MetadataBuilder() {
        ignoredParameterTypeNames.add("javax.servlet.ServletRequest");
        ignoredParameterTypeNames.add("javax.servlet.http.HttpServletRequest");
        ignoredParameterTypeNames.add("jakarta.servlet.ServletRequest");
        ignoredParameterTypeNames.add("jakarta.servlet.http.HttpServletRequest");
        ignoredParameterTypeNames.add("javax.servlet.ServletResponse");
        ignoredParameterTypeNames.add("javax.servlet.http.HttpServletResponse");
        ignoredParameterTypeNames.add("jakarta.servlet.ServletResponse");
        ignoredParameterTypeNames.add("jakarta.servlet.http.HttpServletResponse");
    }

    @Override
    public Metadata.Builder setOperationParser(Metadata.OperationParser operationParser) {
        this.operationParser = operationParser;
        return this;
    }

    @Override
    public Metadata.Builder setParameterParser(Metadata.ParameterParser parameterParser) {
        this.parameterParser = parameterParser;
        return this;
    }

    @Override
    public Metadata.Builder setGroups(Collection<String> groups) {
        if (groups != null && !groups.isEmpty()) {
            Set<String> set = new HashSet<>((groups.size() * 4 + 2) / 3);
            for (String group : groups) {
                String trim = group.trim();
                if (!trim.isEmpty()) {
                    set.add(trim);
                }
                this.groups = set.isEmpty() ? null : set;
            }
        } else {
            this.groups = null;
        }
        return this;
    }

    @Override
    public Metadata.Builder setGenericSupported(boolean genericSupported) {
        this.genericSupported = genericSupported;
        return this;
    }

    @Override
    public Metadata.Builder setUriPrefix(String uriPrefix) {
        this.uriPrefix = uriPrefix;
        return this;
    }

    @Override
    public MetadataBuilder setVirtualTypeMap(Map<TypeName, VirtualType> virtualTypeMap) {
        this.virtualTypeMap =
                virtualTypeMap != null && !virtualTypeMap.isEmpty() ?
                        virtualTypeMap :
                        Collections.emptyMap();
        return this;
    }

    @Override
    public Metadata.Builder addIgnoredParameterTypes(Class<?>... types) {
        ignoredParameterTypes.addAll(Arrays.asList(types));
        return this;
    }

    @Override
    public Metadata.Builder addIllegalReturnTypes(Class<?>... types) {
        illegalReturnTypes.addAll(Arrays.asList(types));
        return this;
    }

    @Override
    public Metadata.Builder addIgnoredParameterTypeNames(String... typeNames) {
        ignoredParameterTypeNames.addAll(Arrays.asList(typeNames));
        return this;
    }

    @Override
    public Metadata.Builder addIllegalReturnTypeNames(String... typeNames) {
        illegalReturnTypeNames.addAll(Arrays.asList(typeNames));
        return this;
    }

    @Override
    public Metadata build() {
        if (operationParser == null) {
            throw new IllegalStateException("Operation parse has not been set");
        }
        if (parameterParser == null) {
            throw new IllegalStateException("ParameterParser parse has not been set");
        }
        Schema schema = loadSchema(groups);
        TypeContext ctx = new TypeContext(schema.getTypeDefinitionMap(), virtualTypeMap, genericSupported);

        List<Service> services = new ArrayList<>();
        for (ApiService apiService : schema.getApiServiceMap().values()) {
            services.add(service(apiService, ctx));
        }

        List<ObjectType> fetchedTypes = new ArrayList<>(ctx.fetchedTypes());
        List<ObjectType> dynamicTypes = new ArrayList<>(ctx.dynamicTypes());
        List<ObjectType> embeddableTypes = new ArrayList<>(ctx.embeddableTypes());
        List<ObjectType> staticTypes = new ArrayList<>();
        for (StaticObjectTypeImpl staticObjectType : ctx.staticTypes()) {
            if (staticObjectType.unwrap() == null) {
                staticTypes.add(staticObjectType);
            }
        }
        List<EnumType> enumTypes = new ArrayList<>(ctx.enumTypes());

        return new MetadataImpl(
                genericSupported,
                Collections.unmodifiableList(services),
                Collections.unmodifiableList(fetchedTypes),
                Collections.unmodifiableList(dynamicTypes),
                Collections.unmodifiableList(embeddableTypes),
                Collections.unmodifiableList(staticTypes),
                Collections.unmodifiableList(enumTypes)
        );
    }

    @SuppressWarnings("unchecked")
    public static Schema loadSchema(Set<String> groups) {
        Map<TypeName, ApiServiceImpl<Void>> serviceMap = new LinkedHashMap<>();
        Map<TypeName, TypeDefinitionImpl<Void>> definitionMap = new LinkedHashMap<>();
        try {
            Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources("META-INF/jimmer/client");
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    Schema schema = Schemas.readFrom(reader, groups);
                    for (ApiService service : schema.getApiServiceMap().values()) {
                        if (groups == null || groups.isEmpty()) {
                            // 没有分组查询的情况 全部放入
                            serviceMap.putIfAbsent(service.getTypeName(), (ApiServiceImpl<Void>) service);
                        } else {
                            // 有分组查询的时候 需要判断service的groups不为null 且 需要包含在groups里
                            if (service.getGroups() != null && groups.containsAll(service.getGroups())) {
                                serviceMap.putIfAbsent(service.getTypeName(), (ApiServiceImpl<Void>) service);
                            }
                        }
                    }
                    for (TypeDefinition definition : schema.getTypeDefinitionMap().values()) {
                        definitionMap.putIfAbsent(definition.getTypeName(), (TypeDefinitionImpl<Void>) definition);
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to load resources \"" + url + "\"", ex);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load resources \"META-INF/jimmer/client\"", ex);
        }
        try {
            Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources("META-INF/jimmer/doc.properties");
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    Properties properties = new Properties();
                    properties.load(reader);
                    for (TypeDefinitionImpl<?> definition : definitionMap.values()) {
                        definition.loadExportDoc(properties);
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to load resources \"" + url + "\"", ex);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load resources \"META-INF/jimmer/doc.properties\"", ex);
        }
        return new SchemaImpl<>(serviceMap, definitionMap);
    }

    private ServiceImpl service(ApiService apiService, TypeContext ctx) {
        ServiceImpl service = new ServiceImpl(ctx.javaType(apiService.getTypeName()));
        service.setDoc(apiService.getDoc());
        String baseUri = operationParser.uri(service.getJavaType());
        if (uriPrefix != null && !uriPrefix.isEmpty()) {
            baseUri = concatUri(uriPrefix, baseUri);
        }
        Map<String, Operation> endpointMap = new LinkedHashMap<>();
        Map<ApiOperation, Operation> operationMap = new IdentityHashMap<>((apiService.getOperations().size() * 4 + 2) / 3);
        for (Method method : service.getJavaType().getMethods()) {
            boolean isSuspendFun = false;
            Parameter[] parameters = method.getParameters();
            if (parameters.length > 0) {
                Class<?> lastParamType = parameters[parameters.length - 1].getType();
                if ("kotlin.coroutines.Continuation".equals(lastParamType.getName())) {
                    isSuspendFun = true;
                }
            }
            ApiOperation apiOperation = null;
            if (isSuspendFun) {
                Parameter[] realParams = Arrays.copyOf(parameters, parameters.length - 1);
                apiOperation = apiService.findOperation(method.getName(), realParams);
            } else {
                apiOperation = apiService.findOperation(method.getName(), parameters);
            }
            if (apiOperation != null) {
                OperationImpl operation = operation(service, apiOperation, method, baseUri, ctx, isSuspendFun);
                operationMap.put(apiOperation, operation);
                for (Operation.HttpMethod httpMethod : operation.getHttpMethods()) {
                    String endpoint = httpMethod.name() + ':' + operation.getUri();
                    Operation conflictOperation = endpointMap.put(endpoint, operation);
                    if (conflictOperation != null) {
                        throw new IllegalApiException(
                                "Conflict endpoint \"" +
                                        endpoint +
                                        "\" which is shared by \"" +
                                        conflictOperation.getJavaMethod() +
                                        "\" and \"" +
                                        operation.getJavaMethod() +
                                        "\""
                        );
                    }
                }
            }
        }
        List<Operation> operations = new ArrayList<>(apiService.getOperations().size());
        for (ApiOperation apiOperation : apiService.getOperations()) {
            Operation operation = operationMap.get(apiOperation);
            if (operation != null) {
                operations.add(operation);
            }
        }
        service.setOperations(Collections.unmodifiableList(operations));
        return service;
    }

    private OperationImpl operation(Service service, ApiOperation apiOperation, Method method, String baseUri, TypeContext ctx, boolean isSuspendFun) {
        OperationImpl operation = new OperationImpl(service, method);
        String uri = operationParser.uri(method);
        operation.setUri(concatUri(baseUri, uri));
        operation.setDoc(apiOperation.getDoc());
        operation.setHttpMethods(operationParser.http(method));
        Parameter[] javaParameters = method.getParameters();
        if (isSuspendFun && javaParameters.length > 0) {
            javaParameters = Arrays.copyOf(javaParameters, javaParameters.length - 1);
        }
        List<org.babyfish.jimmer.client.runtime.Parameter> parameters = new ArrayList<>();
        for (ApiParameter apiParameter : apiOperation.getParameters()) {
            if (!ignoredParameterTypes.contains(javaParameters[apiParameter.getOriginalIndex()].getType()) &&
                    !ignoredParameterTypeNames.contains(javaParameters[apiParameter.getOriginalIndex()].getType().getName())) {
                parameters.add(parameter(apiParameter, javaParameters[apiParameter.getOriginalIndex()], method, ctx));
            }
        }
        boolean hasRequestBody = false;
        boolean hasRequestPart = false;
        for (org.babyfish.jimmer.client.runtime.Parameter parameter : parameters) {
            if (parameter.isRequestBody()) {
                if (hasRequestBody) {
                    throw new IllegalApiException(
                            "Illegal method \"" +
                                    method +
                                    "\", it can't have more than one request body parameter"
                    );
                }
                hasRequestBody = true;
            }
            hasRequestPart |= parameter.getRequestPart() != null;
            if (hasRequestBody && hasRequestPart) {
                throw new IllegalApiException(
                        "Illegal method \"" +
                                method +
                                "\", It can't have both request body and request part parameters"
                );
            }
        }
        operation.setParameters(Collections.unmodifiableList(parameters));
        if (apiOperation.getReturnType() != null) {
            if (illegalReturnTypes.contains(method.getReturnType()) ||
                    illegalReturnTypeNames.contains(method.getReturnType().getName())) {
                throw new IllegalApiException(
                        "Illegal method \"" +
                                method +
                                "\", The client API does not support the operation return type \"" +
                                method.getReturnType().getName() +
                                "\", please change the return type or add `@ApiIgnore` to the current operation"
                );
            }
            if (isSuspendFun) {
                java.lang.reflect.Type[] genericTypes = method.getGenericParameterTypes();
                if (genericTypes.length > 0) {
                    java.lang.reflect.Type lastType = genericTypes[genericTypes.length - 1];
                    String typeName = lastType.getTypeName();
                    if (typeName.startsWith("kotlin.coroutines.Continuation<") && typeName.endsWith(">")) {
                        String realType = typeName.substring(typeName.indexOf('<') + 1, typeName.length() - 1);
                        operation.setReturnType(ctx.parseType(apiOperation.getReturnType()));
                    } else {
                        operation.setReturnType(ctx.parseType(apiOperation.getReturnType()));
                    }
                } else {
                    operation.setReturnType(ctx.parseType(apiOperation.getReturnType()));
                }
            } else {
                operation.setReturnType(ctx.parseType(apiOperation.getReturnType()));
            }
        }
        operation.setExceptionTypes(
                apiOperation
                        .getExceptionTypes()
                        .stream()
                        .map(it -> (ObjectType) ctx.parseType(it))
                        .collect(Collectors.toList())
        );
        return operation;
    }

    private ParameterImpl parameter(ApiParameter apiParameter, Parameter javaParameter, Method method, TypeContext ctx) {
        ParameterImpl parameter = new ParameterImpl(apiParameter.getName());
        Type type = ctx.parseType(apiParameter.getType());
        Type nonNullType = NullableTypeImpl.unwrap(type);
        String requestParam = parameterParser.requestParam(javaParameter);
        String requestHeader = parameterParser.requestHeader(javaParameter);
        String pathVariable = parameterParser.pathVariable(javaParameter);
        boolean isRequestBody = parameterParser.isRequestBody(javaParameter);
        String requestPart = parameterParser.requestPart(javaParameter);
        if (requestPart != null) {
            requestParam = null;
        } else if (parameterParser.isRequestPartRequired(javaParameter)) {
            requestPart = requestParam;
            if (requestPart == null) {
                requestPart = "";
            }
            requestParam = null;
        }
        Set<String> parameterKinds = new LinkedHashSet<>();
        if (requestHeader != null) {
            parameterKinds.add("request header");
        }
        if (requestParam != null) {
            parameterKinds.add("request parameter");
        }
        if (pathVariable != null) {
            parameterKinds.add("path variable");
        }
        if (requestPart != null) {
            parameterKinds.add("request part");
        }
        if (isRequestBody) {
            parameterKinds.add("request body");
        }
        if (parameterKinds.size() > 1) {
            throw new IllegalApiException(
                    "Illegal API method \"" +
                            method +
                            "\", its parameter \"" +
                            apiParameter.getName() +
                            "\" cannot be both " + parameterKinds
            );
        }
        if (requestHeader != null) {
            if (requestHeader.isEmpty()) {
                parameter.setRequestHeader(apiParameter.getName());
            } else {
                parameter.setRequestHeader(requestHeader);
            }
        } else if (requestParam != null) {
            if (requestParam.isEmpty()) {
                parameter.setRequestParam(apiParameter.getName());
            } else {
                parameter.setRequestParam(requestParam);
            }
        } else if (pathVariable != null) {
            if (pathVariable.isEmpty()) {
                parameter.setPathVariable(apiParameter.getName());
            } else {
                parameter.setPathVariable(pathVariable);
            }
        } else if (requestPart != null) {
            if (requestPart.isEmpty()) {
                parameter.setRequestPart(apiParameter.getName());
            } else {
                parameter.setRequestPart(requestPart);
            }
        } else if (isRequestBody) {
            parameter.setRequestBody(true);
        } else if (nonNullType instanceof SimpleType || nonNullType instanceof EnumType) {
            parameter.setRequestParam(parameter.getName());
        } else if (!apiParameter.getType().getTypeName().isGenerationRequired()) {
            throw new IllegalApiException(
                    "Illegal API method \"" +
                            method +
                            "\", its parameter \"" +
                            apiParameter.getName() +
                            "\" is not simple type, but its neither request param nor " +
                            "path variable nor request body"
            );
        }
        if (pathVariable != null && apiParameter.getType().isNullable()) {
            throw new IllegalApiException(
                    "Illegal API method \"" +
                            method +
                            "\", its parameter \"" +
                            apiParameter.getName() +
                            "\" cannot be nullable type because it is path variable"
            );
        }

        String defaultValue = parameterParser.defaultValue(javaParameter);
        parameter.setDefaultValue(defaultValue);

        if (requestHeader != null && !NullableTypeImpl.unwrap(type).equals(SimpleTypeImpl.of(TypeName.STRING))) {
            throw new IllegalApiException(
                    "Illegal API method \"" +
                            method +
                            "\", its parameter \"" +
                            apiParameter.getName() +
                            "\" is http header parameter but its type is not string"
            );
        }
        Boolean optional = parameterParser.isOptional(javaParameter);
        if (Boolean.TRUE.equals(optional)) {
            type = NullableTypeImpl.of(type);
        } else if (Boolean.FALSE.equals(optional)) {
            type = NullableTypeImpl.unwrap(type);
        } else if (optional == null && apiParameter.getType().isNullable() && defaultValue == null) {
            throw new IllegalApiException(
                    "Illegal API method \"" +
                            method +
                            "\", its parameter \"" +
                            apiParameter.getName() +
                            "\" is considered as nullable by jimmer " +
                            "but the web framework thinks it's neither optional nor has a default value, " +
                            "please use web parameter annotation (such as @RequestParam) explicitly"
            );
        }
        parameter.setType(type);
        return parameter;
    }

    private static String concatUri(String baseUri, String uri) {
        if (baseUri == null) {
            baseUri = "";
        }
        if (uri == null) {
            uri = "";
        }
        if (baseUri.isEmpty()) {
            return uri.startsWith("/") ? uri : '/' + uri;
        }
        if (uri.isEmpty()) {
            return baseUri.startsWith("/") ? baseUri : '/' + baseUri;
        }
        if (baseUri.endsWith("/") && uri.startsWith("/")) {
            return baseUri + uri.substring(1);
        }
        if (!baseUri.endsWith("/") && !uri.startsWith("/")) {
            return baseUri + '/' + uri;
        }
        return baseUri + uri;
    }
}
