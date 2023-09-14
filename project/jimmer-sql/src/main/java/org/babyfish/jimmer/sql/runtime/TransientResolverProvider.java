package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.TransientResolver;

public interface TransientResolverProvider extends StrategyProvider<TransientResolver<?, ?>> {

    boolean shouldResolversBeCreatedImmediately();
}
