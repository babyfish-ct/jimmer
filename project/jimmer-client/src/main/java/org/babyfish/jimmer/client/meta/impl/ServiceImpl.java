package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Document;
import org.babyfish.jimmer.client.meta.Operation;
import org.babyfish.jimmer.client.meta.Service;
import org.babyfish.jimmer.client.meta.Visitor;
import org.babyfish.jimmer.impl.asm.Type;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class ServiceImpl implements Service {

    private final Class<?> rawType;

    private final String uri;

    private final Operation.HttpMethod defaultMethod;

    private final Document document;

    List<Operation> operations;

    ServiceImpl(Class<?> rawType, String uri, Operation.HttpMethod defaultMethod) {
        this.rawType = rawType;
        this.document = DocumentImpl.of(rawType);
        this.uri = uri;
        this.defaultMethod = defaultMethod;
    }

    @Override
    public Class<?> getJavaType() {
        return rawType;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public Operation.HttpMethod getDefaultMethod() {
        return defaultMethod;
    }

    @Override
    public List<Operation> getOperations() {
        return operations;
    }

    @Nullable
    @Override
    public Document getDocument() {
        return document;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitingService(this);
        for (Operation operation : operations) {
            operation.accept(visitor);
        }
        visitor.visitedService(this);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("interface ").append(rawType.getSimpleName()).append("{\n");
        for (Operation operation : operations) {
            builder.append('\t').append(operation).append('\n');
        }
        builder.append("}");
        return builder.toString();
    }

    public static Service create(
            Context ctx,
            Class<?> serviceType,
            String uri,
            Operation.HttpMethod defaultMethod
    ) {
        ServiceImpl service = new ServiceImpl(serviceType, uri, defaultMethod);
        List<Operation> list = new ArrayList<>();
        for (Method method : serviceType.getMethods()) {
            Operation operation = OperationImpl.create(ctx, service, method);
            if (operation != null) {
                list.add(operation);
            }
        }
        list.sort(Comparator
                .comparing(Operation::getName)
                .thenComparing(it -> Type.getMethodDescriptor(it.getRawMethod())));
        service.operations = Collections.unmodifiableList(list);
        return service;
    }
}
