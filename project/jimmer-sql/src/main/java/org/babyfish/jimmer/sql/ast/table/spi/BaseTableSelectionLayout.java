package org.babyfish.jimmer.sql.ast.table.spi;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class BaseTableSelectionLayout {

    private static final BaseTableSelectionLayout[] SINGLE_LAYOUTS = {
            new BaseTableSelectionLayout(new BaseTableSelectionKind[]{BaseTableSelectionKind.NON_NULL_TABLE}),
            new BaseTableSelectionLayout(new BaseTableSelectionKind[]{BaseTableSelectionKind.NULLABLE_TABLE}),
            new BaseTableSelectionLayout(new BaseTableSelectionKind[]{BaseTableSelectionKind.NON_NULL_EXPRESSION}),
            new BaseTableSelectionLayout(new BaseTableSelectionKind[]{BaseTableSelectionKind.NULLABLE_EXPRESSION})
    };

    private final BaseTableSelectionKind[] kinds;

    private BaseTableSelectionLayout(BaseTableSelectionKind[] kinds) {
        this.kinds = kinds;
    }

    @NotNull
    public static BaseTableSelectionLayout of(@NotNull BaseTableSelectionKind kind) {
        return SINGLE_LAYOUTS[kind.ordinal()];
    }

    @NotNull
    public static BaseTableSelectionLayout of(@NotNull BaseTableSelectionKind... kinds) {
        if (kinds.length == 1) {
            return of(kinds[0]);
        }
        return new BaseTableSelectionLayout(kinds);
    }

    @NotNull
    public BaseTableSelectionLayout append(@NotNull BaseTableSelectionKind kind) {
        BaseTableSelectionKind[] kinds = Arrays.copyOf(this.kinds, this.kinds.length + 1);
        kinds[this.kinds.length] = kind;
        return new BaseTableSelectionLayout(kinds);
    }

    @NotNull
    public BaseTableSelectionKind get(int index) {
        return kinds[index];
    }
}
