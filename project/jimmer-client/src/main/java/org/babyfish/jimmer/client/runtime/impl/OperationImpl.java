package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.runtime.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class OperationImpl implements Operation {

    private final Service declaringService;

    private final Method javaMethod;

    private Doc doc;

    private String uri;

    private List<HttpMethod> httpMethods;

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
    public List<HttpMethod> getHttpMethods() {
        return httpMethods;
    }

    void setHttpMethods(HttpMethod[] httpMethods) {
        this.httpMethods = Collections.unmodifiableList(
                Arrays.stream(httpMethods).distinct().collect(Collectors.toList())
        );
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
                ", httpMethods=" + httpMethods +
                ", parameters=" + parameters +
                ", exceptionTypes=" + exceptionTypes +
                '}';
    }
}
