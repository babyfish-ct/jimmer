package org.babyfish.jimmer.client.common;

import org.babyfish.jimmer.client.common.PathVariable;
import org.babyfish.jimmer.client.common.RequestBody;
import org.babyfish.jimmer.client.common.RequestParam;
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
    public String defaultValue(Parameter javaParameter) {
        RequestParam requestParam = javaParameter.getAnnotation(RequestParam.class);
        if (requestParam == null || requestParam.defaultVale().isEmpty()) {
            return null;
        }
        return requestParam.defaultVale();
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
