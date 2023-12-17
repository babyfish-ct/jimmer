package org.babyfish.jimmer.client.java.common;

import org.babyfish.jimmer.client.meta.common.PathVariable;
import org.babyfish.jimmer.client.meta.common.RequestBody;
import org.babyfish.jimmer.client.meta.common.RequestParam;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;

public class ParameterParserImpl implements Metadata.ParameterParser {

    @Nullable
    @Override
    public String requestParam(Parameter javaParameter) {
        RequestParam requestParam = javaParameter.getAnnotation(RequestParam.class);
        return requestParam != null ? requestParam.value() : null;
    }

    @Override
    public boolean isDefault(Parameter javaParameter) {
        return false;
    }

    @Nullable
    @Override
    public String pathVariable(Parameter javaParameter) {
        PathVariable pathVariable = javaParameter.getAnnotation(PathVariable.class);
        return pathVariable != null ? pathVariable.value() : null;
    }

    @Override
    public boolean isRequestBody(Parameter javaParameter) {
        return javaParameter.getAnnotation(RequestBody.class) != null;
    }
}
