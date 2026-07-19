package org.babyfish.jimmer.spring.validation;

import jakarta.validation.Path;
import jakarta.validation.TraversableResolver;

import java.lang.annotation.ElementType;

final class JakartaCompositeTraversableResolver implements TraversableResolver {

    private final TraversableResolver first;

    private final TraversableResolver second;

    JakartaCompositeTraversableResolver(TraversableResolver first, TraversableResolver second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean isReachable(
            Object traversableObject,
            Path.Node traversableProperty,
            Class<?> rootBeanType,
            Path pathToTraversableObject,
            ElementType elementType
    ) {
        return first.isReachable(
                traversableObject,
                traversableProperty,
                rootBeanType,
                pathToTraversableObject,
                elementType
        ) && second.isReachable(
                traversableObject,
                traversableProperty,
                rootBeanType,
                pathToTraversableObject,
                elementType
        );
    }

    @Override
    public boolean isCascadable(
            Object traversableObject,
            Path.Node traversableProperty,
            Class<?> rootBeanType,
            Path pathToTraversableObject,
            ElementType elementType
    ) {
        return first.isCascadable(
                traversableObject,
                traversableProperty,
                rootBeanType,
                pathToTraversableObject,
                elementType
        ) && second.isCascadable(
                traversableObject,
                traversableProperty,
                rootBeanType,
                pathToTraversableObject,
                elementType
        );
    }
}
