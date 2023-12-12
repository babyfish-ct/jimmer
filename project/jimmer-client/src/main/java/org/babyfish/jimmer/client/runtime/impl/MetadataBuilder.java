package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.client.meta.impl.ApiServiceImpl;
import org.babyfish.jimmer.client.meta.impl.SchemaImpl;
import org.babyfish.jimmer.client.meta.impl.Schemas;
import org.babyfish.jimmer.client.meta.impl.TypeDefinitionImpl;
import org.babyfish.jimmer.client.runtime.EnumType;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Operation;
import org.babyfish.jimmer.client.runtime.Service;
import org.babyfish.jimmer.client.runtime.Type;

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
            this.groups = new HashSet<>(groups);
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
    public Metadata build() {
        if (operationParser == null) {
            throw new IllegalStateException("Operation parse has not been set");
        }
        Schema schema = loadSchema(groups);
        TypeContext ctx = new TypeContext(schema.getTypeDefinitionMap(), genericSupported);

        List<Service> services = new ArrayList<>();
        for (ApiService apiService : schema.getApiServiceMap().values()) {
            services.add(service(apiService, ctx));
        }

        List<ObjectType> fetchedTypes = new ArrayList<>();
        List<ObjectType> dynamicTypes = new ArrayList<>();
        List<ObjectType> staticTypes = new ArrayList<>();

        for (ImmutableObjectTypeImpl immutableObjectType : ctx.immutableObjectTypes()) {
            if (immutableObjectType.getFetchBy() != null) {
                fetchedTypes.add(immutableObjectType);
            } else {
                dynamicTypes.add(immutableObjectType);
            }
        }
        for (StaticObjectTypeImpl staticObjectType : ctx.staticObjectTypes()) {
            if (!genericSupported || staticObjectType.getArguments().isEmpty()) {
                staticTypes.add(staticObjectType);
            }
        }
        List<EnumType> enumTypes = new ArrayList<>(ctx.enumTypes());

        return new MetadataImpl(
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
        List<Operation> operations = new ArrayList<>(apiService.getOperations().size());
        for (Method method : service.getJavaType().getMethods()) {
            ApiOperation apiOperation = apiService.findOperation(method.getName(), method.getParameterTypes());
            if (apiOperation != null) {
                OperationImpl operation = operation(apiOperation, method, baseUri, ctx);
                operations.add(operation);
            }
        }
        service.setOperations(Collections.unmodifiableList(operations));
        return service;
    }

    private OperationImpl operation(ApiOperation apiOperation, Method method, String baseUri, TypeContext ctx) {
        OperationImpl operation = new OperationImpl(method);
        String uri = operationParser.uri(method);
        operation.setUri(concatUri(baseUri, uri));
        operation.setDoc(apiOperation.getDoc());
        operation.setHttpMethod(operationParser.http(method));
        Parameter[] javaParameters = method.getParameters();
        List<org.babyfish.jimmer.client.runtime.Parameter> parameters = new ArrayList<>();
        for (ApiParameter apiParameter : apiOperation.getParameters()) {
            parameters.add(parameter(apiParameter, javaParameters[apiParameter.getOriginalIndex()], method, ctx));
        }
        operation.setParameters(Collections.unmodifiableList(parameters));
        if (apiOperation.getReturnType() != null) {
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
        String requestParam = parameterParser.requestParam(javaParameter);
        if (requestParam != null && !requestParam.isEmpty()) {
            parameter.setRequestParam(requestParam);
        } else {
            String pathVariable = parameterParser.pathVariable(javaParameter);
            if (pathVariable != null && !pathVariable.isEmpty()) {
                parameter.setPathVariable(pathVariable);
            } else if (parameterParser.isRequestBody(javaParameter)) {
                parameter.setRequestBody(true);
            } else {
                throw new IllegalApiException(
                        "Illegal API method \"" +
                                method +
                                "\", its parameter \"" +
                                apiParameter.getName() +
                                "\" is neither request param nor " +
                                "path variable nor request body"
                );
            }
        }
        Type type = ctx.parseType(apiParameter.getType());
        if (!apiParameter.getType().isNullable() &&
                (apiParameter.isDefaultValueSpecified() || parameterParser.isDefault(javaParameter))) {
            parameter.setType(NullableTypeImpl.of(type));
        } else {
            parameter.setType(type);
        }
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
