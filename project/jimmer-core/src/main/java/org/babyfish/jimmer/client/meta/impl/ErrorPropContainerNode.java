package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Prop;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ErrorPropContainerNode<S> extends AstNode<S> {

    private final Map<String, PropImpl<S>> errorPropMap = new LinkedHashMap<>();

    ErrorPropContainerNode(S source) {
        super(source);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Prop> getErrorPropMap() {
        return (Map<String, Prop>) (Map<?, ?>) errorPropMap;
    }

    public void addErrorProp(PropImpl<S> errorProp) {
        errorPropMap.put(errorProp.getName(), errorProp);
    }
}
