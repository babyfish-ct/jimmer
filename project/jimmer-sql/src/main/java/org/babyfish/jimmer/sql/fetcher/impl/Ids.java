package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.runtime.ImmutableSpi;

public class Ids {

    private Ids() {}

    public static Object idOf(ImmutableSpi spi) {
        if (spi == null) {
            return null;
        }
        return spi.__get(spi.__type().getIdProp().getName());
    }
}
