package org.babyfish.jimmer.spring.validation;

import org.babyfish.jimmer.runtime.ImmutableSpi;

import javax.validation.Path;
import javax.validation.TraversableResolver;
import java.lang.annotation.ElementType;

/**
 * Prevents Bean Validation from traversing unloaded Jimmer immutable properties.
 * This resolver does not delegate; combine it with other resolvers when necessary.
 */
public final class JimmerJavaxTraversableResolver implements TraversableResolver {

    @Override
    public boolean isReachable(
            Object traversableObject,
            Path.Node traversableProperty,
            Class<?> rootBeanType,
            Path pathToTraversableObject,
            ElementType elementType
    ) {
        return !(traversableObject instanceof ImmutableSpi) ||
                traversableProperty == null ||
                traversableProperty.getName() == null ||
                ((ImmutableSpi) traversableObject).__isLoaded(traversableProperty.getName());
    }

    @Override
    public boolean isCascadable(
            Object traversableObject,
            Path.Node traversableProperty,
            Class<?> rootBeanType,
            Path pathToTraversableObject,
            ElementType elementType
    ) {
        return true;
    }
}
