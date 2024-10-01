package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScalarProviderUtils {
    @Nullable
    public static <T> Object toSql(@NotNull T literal,
                                   @NotNull ScalarProvider<T, ?> scalarProvider,
                                   @NotNull Dialect dialect) throws Exception {
        Object sqlValue = scalarProvider.toSql(literal);
        return scalarProvider.isJsonScalar() ? dialect.jsonToBaseValue((String) sqlValue) : sqlValue;
    }
}
