package org.babyfish.jimmer.sql.ast.table.spi;

import org.jetbrains.annotations.NotNull;

public interface WeakJoinMetadataParser {

    @NotNull
    WeakJoinMetadata parse(@NotNull Class<?> weakJoinType);
}
