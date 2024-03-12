package org.babyfish.jimmer.client.runtime;

import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.runtime.impl.MetadataBuilder;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Metadata {

    boolean isGenericSupported();

    Map<String, List<Operation>> getPathMap();

    List<Service> getServices();

    List<ObjectType> getFetchedTypes();

    List<ObjectType> getDynamicTypes();

    List<ObjectType> getEmbeddableTypes();

    List<ObjectType> getStaticTypes();

    List<EnumType> getEnumTypes();

    Type getType(Class<?> type);

    static Builder newBuilder() {
        return new MetadataBuilder();
    }

    interface Builder {

        Builder setOperationParser(OperationParser operationParser);

        Builder setParameterParameter(ParameterParser parameterParser);

        Builder setGroups(Collection<String> groups);

        Builder setGenericSupported(boolean genericSupported);

        Builder setUriPrefix(String uriPrefix);

        Builder setControllerNullityChecked(boolean checked);

        Builder setVirtualTypeMap(Map<TypeName, VirtualType> virtualTypeMap);

        Builder addIgnoredParameterTypes(Class<?>... types);

        Builder addIllegalReturnTypes(Class<?>... types);

        Metadata build();
    }

    interface OperationParser {

        String uri(AnnotatedElement element);

        Operation.HttpMethod[] http(Method method);

        boolean isStream(Method method);
    }

    interface ParameterParser {

        @Nullable
        String requestHeader(Parameter javaParameter);

        @Nullable
        String requestParam(Parameter javaParameter);

        @Nullable
        String pathVariable(Parameter javaParameter);

        @Nullable
        String requestPart(Parameter javaParameter);

        @Nullable
        String defaultValue(Parameter javaParameter);

        boolean isOptional(Parameter javaParameter);

        boolean isRequestBody(Parameter javaParameter);

        boolean isRequestPartRequired(Parameter javaParameter);
    }
}
