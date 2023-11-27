package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Metadata;
import org.babyfish.jimmer.client.meta.Operation;
import org.babyfish.jimmer.client.meta.Service;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.util.*;

public class MetadataBuilder implements Metadata.Builder {

    private final Set<Class<?>> serviceTypes = new HashSet<>();

    private Metadata.OperationParser operationParser;

    private Metadata.ParameterParser parameterParser;

    @Override
    public Metadata.Builder addServiceTypes(Collection<Class<?>> serviceTypes) {
        for (Class<?> serviceType : serviceTypes) {
            if (serviceType != null) {
                this.serviceTypes.add(serviceType);
            }
        }
        return this;
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
    public Metadata build() {
        if (operationParser == null) {
            throw new IllegalStateException("operationParser has not been set");
        }
        if (parameterParser == null) {
            throw new IllegalStateException("parameterParser has not been set");
        }
        Context ctx = new Context(operationParser, parameterParser);
        Map<Class<?>, Service> serviceMap = new LinkedHashMap<>();
        for (Class<?> serviceType : serviceTypes) {
            Tuple2<String, Operation.HttpMethod> tuple = operationParser.http(serviceType);
            String uri = null;
            Operation.HttpMethod defaultMethod = Operation.HttpMethod.GET;
            if (tuple != null) {
                if (!tuple.get_1().isEmpty()) {
                    uri = tuple.get_1();
                }
                if (tuple.get_2() != null) {
                    defaultMethod = tuple.get_2();
                }
            }
            Service service = ServiceImpl.create(
                    ctx,
                    serviceType,
                    uri,
                    defaultMethod
            );
            if (service != null) {
                serviceMap.put(serviceType, service);
            }
        }
        return new MetadataImpl(
                serviceMap,
                ctx.getGenericTypes(),
                ctx.staticObjectTypeMap,
                ctx.enumTypeMap,
                ctx.fetchedImmutableObjectTypeMap,
                ctx.viewImmutableObjectTypeMap,
                ctx.rawImmutableObjectTypeMap
        );
    }
}
