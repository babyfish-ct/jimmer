package org.babyfish.jimmer.client.meta;

import org.babyfish.jimmer.client.meta.impl.MetadataBuilder;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

public interface Metadata {

    Map<Class<?>, Service> getServices();

    Map<Class<?>, StaticObjectType> getGenericTypes();

    Map<StaticObjectType.Key, StaticObjectType> getStaticTypes();

    Map<Class<?>, EnumType> getEnumTypes();

    Map<Fetcher<?>, ImmutableObjectType> getFetchedImmutableObjectTypes();

    Map<ImmutableType, ImmutableObjectType> getViewImmutableObjectTypes();

    Map<ImmutableType, ImmutableObjectType> getRawImmutableObjectTypes();

    static Builder newBuilder() {
        return new MetadataBuilder();
    }

    interface Builder {

        default Builder addService(Object service) {
            return addServices(Collections.singleton(service));
        }

        default Builder addServices(Object ... services) {
            return addServices(Arrays.asList(services));
        }

        default Builder addServices(Collection<Object> services) {
            return addServiceTypes(
                    services
                            .stream()
                            .filter(Objects::nonNull)
                            .map(Object::getClass)
                            .collect(Collectors.toList())
            );
        }

        default Builder addServiceType(Class<?> serviceType) {
            return addServiceTypes(Collections.singleton(serviceType));
        }

        default Builder addServiceTypes(Class<?> ... serviceTypes) {
            return addServiceTypes(Arrays.asList(serviceTypes));
        }

        Builder addServiceTypes(Collection<Class<?>> serviceTypes);

        Builder setOperationParser(OperationParser operationParser);

        Builder setParameterParser(ParameterParser parameterParser);

        Metadata build();
    }

    interface OperationParser {

        Tuple2<String, Operation.HttpMethod> http(AnnotatedElement typeOfMethod);
    }

    interface ParameterParser {

        @Nullable
        Tuple2<String, Boolean> requestParamNameAndNullable(Parameter javaParameter);

        @Nullable
        String pathVariableName(Parameter javaParameter);

        boolean isRequestBody(Parameter javaParameter);

        default boolean isOptional(Parameter javaParameter) {
            return false;
        }
    }
}
