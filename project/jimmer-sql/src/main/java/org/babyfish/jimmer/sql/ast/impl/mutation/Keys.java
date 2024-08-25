package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.LockMode;
import org.babyfish.jimmer.sql.ast.query.MutableQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.IdOnlyFetchType;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.SaveException;

import java.util.*;
import java.util.function.BiConsumer;

class Keys {

    private Keys() {}

    static Object keyOf(ImmutableSpi spi, Collection<ImmutableProp> keyProps) {
        if (keyProps.size() == 1) {
            PropId propId = keyProps.iterator().next().getId();
            return spi.__get(propId);
        }
        Object[] arr = new Object[keyProps.size()];
        int index = 0;
        for (ImmutableProp keyProp : keyProps) {
            Object o = spi.__get(keyProp.getId());
            if (o != null && keyProp.isReference(TargetLevel.PERSISTENT)) {
                o = ((ImmutableSpi)o).__get(keyProp.getTargetType().getIdProp().getId());
            }
            arr[index++] = o;
        }
        return Tuples.valueOf(arr);
    }
}
