package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public interface PropScalarProviderFactory {

    ScalarProvider<?, ?> createScalarProvider(ImmutableProp prop);

    static PropScalarProviderFactory combine(
            PropScalarProviderFactory a,
            PropScalarProviderFactory b
    ) {
        return CombinedPropScalarProviderFactory.combine(a, b);
    }
}

class CombinedPropScalarProviderFactory implements PropScalarProviderFactory {

    private static final PropScalarProviderFactory[] EMPTY_ARR = new PropScalarProviderFactory[0];

    final PropScalarProviderFactory[] arr;

    private CombinedPropScalarProviderFactory(PropScalarProviderFactory[] arr) {
        this.arr = arr;
    }

    static PropScalarProviderFactory combine(
            PropScalarProviderFactory a,
            PropScalarProviderFactory b
    ) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        Set<PropScalarProviderFactory> set = new LinkedHashSet<>();
        if (a instanceof CombinedPropScalarProviderFactory) {
            set.addAll(Arrays.asList(((CombinedPropScalarProviderFactory) a).arr));
        } else {
            set.add(a);
        }
        if (b instanceof CombinedPropScalarProviderFactory) {
            set.addAll(Arrays.asList(((CombinedPropScalarProviderFactory) b).arr));
        } else {
            set.add(b);
        }
        if (set.isEmpty()) {
            return null;
        }
        if (set.size() == 1) {
            return set.iterator().next();
        }
        return new CombinedPropScalarProviderFactory(set.toArray(EMPTY_ARR));
    }

    @Override
    public ScalarProvider<?, ?> createScalarProvider(ImmutableProp prop) {
        ScalarProvider<?, ?> provider = null;
        PropScalarProviderFactory factory = null;
        for (PropScalarProviderFactory f : arr) {
            ScalarProvider<?, ?> p = f.createScalarProvider(prop);
            if (p == null) {
                continue;
            }
            if (provider == null) {
                provider = p;
                factory = f;
            } else if (!provider.equals(p)) {
                throw new IllegalStateException(
                        "Conflict PropScalarProviderFactories \"" +
                                factory +
                                "\" and \"" +
                                f +
                                "\", they create different scalar provider for the property \"" +
                                prop +
                                "\""
                );
            }
        }
        return provider;
    }
}