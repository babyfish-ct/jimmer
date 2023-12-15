package org.babyfish.jimmer.client.java.common;

import org.babyfish.jimmer.client.runtime.Metadata;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;

public class ParameterParserImpl implements Metadata.ParameterParser {
    @Nullable
    @Override
    public String requestParam(Parameter javaParameter) {
        return null;
    }

    @Override
    public boolean isDefault(Parameter javaParameter) {
        return false;
    }

    @Nullable
    @Override
    public String pathVariable(Parameter javaParameter) {
        return null;
    }

    @Override
    public boolean isRequestBody(Parameter javaParameter) {
        return false;
    }
}
