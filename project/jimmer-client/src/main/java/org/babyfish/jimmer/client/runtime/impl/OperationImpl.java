package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.runtime.*;

import java.lang.reflect.Method;
import java.util.List;

public class OperationImpl implements Operation {

    private final Service declaringService;

    private final Method javaMethod;

    private Doc doc;

    private String uri;

    private HttpMethod httpMethod;

    private List<Parameter> parameters;

    private Type returnType;

    private List<ObjectType> exceptionTypes;

    public OperationImpl(Service declaringService, Method javaMethod) {
        this.declaringService = declaringService;
        this.javaMethod = javaMethod;
    }

    @Override
    public Service getDeclaringService() {
        return declaringService;
    }

    @Override
    public String getName() {
        return javaMethod.getName();
    }

    @Override
    public Doc getDoc() {
        return doc;
    }

    void setDoc(Doc doc) {
        this.doc = doc;
    }

    @Override
    public String getUri() {
        return uri;
    }

    void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    @Override
    public Method getJavaMethod() {
        return javaMethod;
    }

    @Override
    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Type getReturnType() {
        return returnType;
    }

    void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    @Override
    public List<ObjectType> getExceptionTypes() {
        return exceptionTypes;
    }

    void setExceptionTypes(List<ObjectType> exceptionTypes) {
        this.exceptionTypes = exceptionTypes;
    }

    @Override
    public String toString() {
        return "OperationImpl{" +
                "javaMethod=" + javaMethod +
                ", doc=" + doc +
                ", uri='" + uri + '\'' +
                ", httpMethod=" + httpMethod +
                ", parameters=" + parameters +
                ", exceptionTypes=" + exceptionTypes +
                '}';
    }
}
