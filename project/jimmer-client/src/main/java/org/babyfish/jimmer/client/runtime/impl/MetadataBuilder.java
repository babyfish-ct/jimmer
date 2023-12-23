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
import java.util.*;
import java.util.stream.Collectors;

public class MetadataBuilder implements Metadata.Builder {

    private Metadata.OperationParser operationParser;

    private Metadata.ParameterParser parameterParser;

    private Set<String> groups;

    private boolean genericSupported;

    private Set<Class<?>> ignoredParameterTypes = new LinkedHashSet<>();

    private Set<Class<?>> illegalReturnTypes = new LinkedHashSet<>();

    @Override
    public Metadata.Builder setOperationParser(Metadata.OperationParser operationParser) {
        this.operationParser = operationParser;
        return this;
    }

    @Override
    public Metadata.Builder setParameterParameter(Metadata.ParameterParser parameterParser) {
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
    public Metadata.Builder addIgnoredParameterTypes(Class<?> ... types) {
        ignoredParameterTypes.addAll(Arrays.asList(types));
        return this;
    }

    @Override
    public Metadata.Builder addIllegalReturnTypes(Class<?>... types) {
        illegalReturnTypes.addAll(Arrays.asList(types));
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
        TypeContext ctx = new TypeContext(schema.getTypeDefinitionMap(), genericSupported);

        List<Service> services = new ArrayList<>();
        for (ApiService apiService : schema.getApiServiceMap().values()) {
            services.add(service(apiService, ctx));
        }

        List<ObjectType> fetchedTypes = new ArrayList<>(ctx.fetchedTypes());
        List<ObjectType> dynamicTypes = new ArrayList<>(ctx.dynamicTypes());
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
                Collections.unmodifiableList(staticTypes),
                Collections.unmodifiableList(enumTypes)
        );
    }

    @SuppressWarnings("unchecked")
    private Schema loadSchema(Set<String> groups) {
        Map<TypeName, ApiServiceImpl<Void>> serviceMap = new LinkedHashMap<>();
        Map<TypeName, TypeDefinitionImpl<Void>> definitionMap = new LinkedHashMap<>();
        try {
            Enumeration<URL> urls = TypeContext.class.getClassLoader().getResources("META-INF/jimmer/client");
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    Schema schema = Schemas.readFrom(reader, groups);
                    for (ApiService service : schema.getApiServiceMap().values()) {
                        serviceMap.putIfAbsent(service.getTypeName(), (ApiServiceImpl<Void>) service);
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
        return new SchemaImpl<>(serviceMap, definitionMap);
    }

    private ServiceImpl service(ApiService apiService, TypeContext ctx) {
        ServiceImpl service = new ServiceImpl(ctx.javaType(apiService.getTypeName()));
        service.setDoc(apiService.getDoc());
        String baseUri = operationParser.uri(service.getJavaType());
        Map<ApiOperation, Operation> operationMap = new IdentityHashMap<>((apiService.getOperations().size() * 4 + 2) / 3);
        for (Method method : service.getJavaType().getMethods()) {
            ApiOperation apiOperation = apiService.findOperation(method.getName(), method.getParameterTypes());
            if (apiOperation != null) {
                OperationImpl operation = operation(service, apiOperation, method, baseUri, ctx);
                operationMap.put(apiOperation, operation);
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

    private OperationImpl operation(Service service, ApiOperation apiOperation, Method method, String baseUri, TypeContext ctx) {
        OperationImpl operation = new OperationImpl(service, method);
        String uri = operationParser.uri(method);
        operation.setUri(concatUri(baseUri, uri));
        operation.setDoc(apiOperation.getDoc());
        operation.setHttpMethod(operationParser.http(method));
        Parameter[] javaParameters = method.getParameters();
        List<org.babyfish.jimmer.client.runtime.Parameter> parameters = new ArrayList<>();
        for (ApiParameter apiParameter : apiOperation.getParameters()) {
            if (!ignoredParameterTypes.contains(javaParameters[apiParameter.getOriginalIndex()].getType())) {
                parameters.add(parameter(apiParameter, javaParameters[apiParameter.getOriginalIndex()], method, ctx));
            }
        }
        operation.setParameters(Collections.unmodifiableList(parameters));
        if (apiOperation.getReturnType() != null) {
            if (illegalReturnTypes.contains(method.getReturnType())) {
                throw new IllegalStateException(
                        "Illegal method \"" +
                                method +
                                "\", The client API does not support the operation return type \"" +
                                method.getReturnType().getName() +
                                "\", please change the return type or add `@ApiIgnore` to the current operation"
                );
            }
            operation.setReturnType(ctx.parseType(apiOperation.getReturnType()));
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
        String requestHeader = parameterParser.requestHeader(javaParameter);
        if (requestHeader != null) {
            if (requestHeader.isEmpty()) {
                parameter.setRequestHeader(apiParameter.getName());
            } else {
                parameter.setRequestHeader(requestHeader);
            }
        } else {
            String requestParam = parameterParser.requestParam(javaParameter);
            if (requestParam != null) {
                if (requestParam.isEmpty()) {
                    parameter.setRequestParam(apiParameter.getName());
                } else {
                    parameter.setRequestParam(requestParam);
                }
            } else {
                String pathVariable = parameterParser.pathVariable(javaParameter);
                if (pathVariable != null) {
                    if (pathVariable.isEmpty()) {
                        parameter.setPathVariable(apiParameter.getName());
                    } else {
                        parameter.setPathVariable(pathVariable);
                    }
                } else if (parameterParser.isRequestBody(javaParameter)) {
                    parameter.setRequestBody(true);
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
            }
        }
        Type type = ctx.parseType(apiParameter.getType());
        if (requestHeader != null && !NullableTypeImpl.unwrap(type).equals(SimpleTypeImpl.of(TypeName.STRING))) {
            throw new IllegalApiException(
                    "Illegal API method \"" +
                            method +
                            "\", its parameter \"" +
                            apiParameter.getName() +
                            "\" is http header parameter but its type is not string"
            );
        }
        if (parameterParser.isOptional(javaParameter)) {
            type = NullableTypeImpl.of(type);
        }
        parameter.setType(type);
        parameter.setDefaultValue(parameterParser.defaultValue(javaParameter));
        return parameter;
    }

    private static String concatUri(String baseUri, String uri) {
        if (baseUri == null) {
            baseUri = "";
        }
        if (uri == null) {
            uri = "";
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
