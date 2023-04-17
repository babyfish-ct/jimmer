package org.babyfish.jimmer.sql.loader.graphql.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.loader.AbstractDataLoader;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

import java.sql.Connection;

class DataLoader extends AbstractDataLoader {

    public DataLoader(
            JSqlClient sqlClient,
            Connection con,
            ImmutableProp prop,
            FieldFilter<?> filter
    ) {
        this(
                sqlClient,
                con,
                prop,
                filter,
                Integer.MAX_VALUE,
                0);
    }

    public DataLoader(
            JSqlClient sqlClient,
            Connection con,
            ImmutableProp prop,
            FieldFilter<?> filter,
            int limit,
            int offset
    ) {
        super(
                sqlClient,
                con,
                null,
                prop,
                prop.isAssociation(TargetLevel.ENTITY) ?
                        targetFetcher(prop.getTargetType()) :
                        null,
                filter,
                limit,
                offset
        );
    }

    private static Fetcher<?> targetFetcher(ImmutableType targetType) {
        Fetcher<?> fetcher = new FetcherImpl<>(targetType.getJavaClass()).allTableFields();
        for (ImmutableProp prop : targetType.getProps().values()) {
            // Note, GraphQL only support formula property based on java/kotlin expression
            if (prop.isFormula() && !(prop.getSqlTemplate() instanceof FormulaTemplate)) {
                fetcher = fetcher.add(prop.getName());
            }
        }
        return fetcher;
    }
}
