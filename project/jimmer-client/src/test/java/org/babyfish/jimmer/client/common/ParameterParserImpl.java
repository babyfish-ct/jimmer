package org.babyfish.jimmer.client.common;

import org.babyfish.jimmer.client.runtime.Metadata;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Parameter;

public class ParameterParserImpl implements Metadata.ParameterParser {

    @Nullable
    @Override
    public String requestHeader(Parameter javaParameter) {
        RequestHeader requestHeader = javaParameter.getAnnotation(RequestHeader.class);
        return requestHeader != null ? requestHeader.value() : null;
    }

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

    @Nullable
    @Override
    public String requestPart(Parameter javaParameter) {
        RequestPart requestPart = javaParameter.getAnnotation(RequestPart.class);
        return requestPart != null ? requestPart.value() : null;
    }

    @Override
    public boolean isOptional(Parameter javaParameter) {
        RequestHeader requestHeader = javaParameter.getAnnotation(RequestHeader.class);
        if (requestHeader != null) {
            return !requestHeader.required();
        }
        RequestParam requestParam = javaParameter.getAnnotation(RequestParam.class);
        if (requestParam != null) {
            return !requestParam.required();
        }
        RequestPart requestPart = javaParameter.getAnnotation(RequestPart.class);
        if (requestPart != null) {
            return !requestPart.required();
        }
        return false;
    }

    @Override
    public boolean isRequestBody(Parameter javaParameter) {
        return javaParameter.getAnnotation(RequestBody.class) != null;
    }

    @Override
    public boolean isRequestPartRequired(Parameter javaParameter) {
        Class<?> type = javaParameter.getType();
        return type == MultipartFile.class || type == MultipartFile[].class;
    }
}
