package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.runtime.Operation;
import org.babyfish.jimmer.client.runtime.Service;

import java.util.List;

public class ServiceImpl implements Service {

    private final Class<?> javaType;

    private Doc doc;

    private List<Operation> operations;

    public ServiceImpl(Class<?> javaType) {
        this.javaType = javaType;
    }

    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    @Override
    public Doc getDoc() {
        return doc;
    }

    void setDoc(Doc doc) {
        this.doc = doc;
    }

    public List<Operation> getOperations() {
        return operations;
    }

    void setOperations(List<Operation> operations) {
        this.operations = operations;
    }

    @Override
    public String toString() {
        return "ServiceImpl{" +
                "javaType=" + javaType.getName() +
                ", doc=" + doc +
                ", operations=" + operations +
                '}';
    }
}
