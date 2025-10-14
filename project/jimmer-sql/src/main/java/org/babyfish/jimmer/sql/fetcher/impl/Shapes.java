package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.jackson.Converter;
import org.babyfish.jimmer.meta.Dependency;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.*;
import java.util.function.Function;

public class Shapes {

    private Shapes() {}

    @SuppressWarnings("unchecked")
    public static <E> void reshape(
            JSqlClientImplementor sqlClient,
            Connection con,
            List<E> entities,
            Fetcher<?> fetcher,
            Function<?, E> converter
    ) {
        if (entities.isEmpty() || fetcher == null) {
            return;
        }
        ImmutableType immutableType = fetcher.getImmutableType();
        List<PropId> shownPropIds = new ArrayList<>();
        for (Field field : fetcher.getFieldMap().values()) {
            if (field.getProp().isView() || field.getProp().isFormula()) {
                shownPropIds.add(field.getProp().getId());
            }
        }
        boolean needDrop = false;
        for (ImmutableSpi spi : (List<ImmutableSpi>) entities) {
            for (ImmutableProp prop : immutableType.getProps().values()) {
                if (spi.__isLoaded(prop.getId())) {
                    Field field = fetcher.getFieldMap().get(prop.getName());
                    if (field == null || field.isImplicit()) {
                        needDrop = true;
                        break;
                    }
                }
            }
        }
        if (needDrop) {
            ListIterator<ImmutableSpi> itr = (ListIterator<ImmutableSpi>) entities.listIterator();
            while (itr.hasNext()) {
                ImmutableSpi spi = itr.next();
                itr.set(
                        (ImmutableSpi) Internal.produce(immutableType, spi, draft -> {
                            for (ImmutableProp prop : immutableType.getProps().values()) {
                                if (spi.__isLoaded(prop.getId())) {
                                    Field field = fetcher.getFieldMap().get(prop.getName());
                                    if (field == null) {
                                        ((DraftSpi) draft).__unload(prop.getId());
                                    } else if (field.isImplicit()) {
                                        ((DraftSpi) draft).__show(prop.getId(), false);
                                    }
                                }
                                for (PropId shownPropId : shownPropIds) {
                                    ((DraftSpi) draft).__show(shownPropId, true);
                                }
                            }
                        })
                );
            }
        }
        FetcherUtil.fetch(
                sqlClient,
                con,
                Collections.singletonList(
                        new FetcherSelection<E>() {

                            @Override
                            public FetchPath getPath() {
                                return null;
                            }

                            @Override
                            public Fetcher<?> getFetcher() {
                                return fetcher;
                            }

                            @Override
                            public PropExpression.Embedded<?> getEmbeddedPropExpression() {
                                return null;
                            }

                            @Override
                            public @Nullable Function<?, E> getConverter() {
                                return converter;
                            }
                        }
                ),
                null,
                entities
        );
    }
}
