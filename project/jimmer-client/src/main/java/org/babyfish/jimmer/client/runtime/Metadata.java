package org.babyfish.jimmer.client.runtime;

import org.babyfish.jimmer.client.runtime.impl.MetadataBuilder;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;

public interface Metadata {

    List<Service> getServices();

    List<ObjectType> getFetchedTypes();

    List<ObjectType> getDynamicTypes();

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

        Metadata build();
    }

    interface OperationParser {

        String uri(AnnotatedElement element);

        Operation.HttpMethod http(Method method);
    }

    interface ParameterParser {

        @Nullable
        String requestParam(Parameter javaParameter);

        boolean isDefault(Parameter javaParameter);

        @Nullable
        String pathVariable(Parameter javaParameter);

        boolean isRequestBody(Parameter javaParameter);
    }
}
