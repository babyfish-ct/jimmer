package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Document;
import org.babyfish.jimmer.client.meta.Operation;
import org.babyfish.jimmer.client.meta.Service;
import org.babyfish.jimmer.client.meta.Visitor;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ServiceImpl implements Service {

    private final Class<?> rawType;

    private final Document document;

    List<Operation> operations;

    ServiceImpl(Class<?> rawType) {
        this.rawType = rawType;
        this.document = DocumentImpl.of(rawType);
    }

    @Override
    public Class<?> getJavaType() {
        return rawType;
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

    public static Service create(Context ctx, Class<?> serviceType) {
        ServiceImpl service = new ServiceImpl(serviceType);
        List<Operation> list = new ArrayList<>();
        for (Method method : serviceType.getMethods()) {
            Operation operation = OperationImpl.create(ctx, service, method);
            if (operation != null) {
                list.add(operation);
            }
        }
        service.operations = Collections.unmodifiableList(list);
        return service;
    }
}
