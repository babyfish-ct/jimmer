package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.meta.impl.SequenceIdGenerator;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class SaveReturningColumnValue {

    final PropertyGetter getter;

    final SaveReturningValueMode mode;

    @Nullable
    final SequenceIdGenerator sequenceIdGenerator;

    SaveReturningColumnValue(
            PropertyGetter getter,
            SaveReturningValueMode mode,
            @Nullable SequenceIdGenerator sequenceIdGenerator
    ) {
        this.getter = getter;
        this.mode = mode;
        this.sequenceIdGenerator = sequenceIdGenerator;
    }

    static void addIfAbsent(
            List<SaveReturningColumnValue> sourceValues,
            PropertyGetter getter,
            SaveReturningValueMode mode
    ) {
        for (SaveReturningColumnValue sourceValue : sourceValues) {
            if (sourceValue.getter.prop().toOriginal() == getter.prop().toOriginal()) {
                return;
            }
        }
        sourceValues.add(new SaveReturningColumnValue(getter, mode, null));
    }

    void appendValue(SqlBuilder builder, DraftSpi draft) {
        switch (mode) {
            case VALUE:
                Object value = getter.get(draft);
                if (value != null) {
                    builder.variable(value);
                } else {
                    builder.nullVariable(getter.prop());
                }
                break;
            case DEFAULT:
                Object defaultValue = getter.metadata().getDefaultValue();
                if (defaultValue != null) {
                    builder.variable(defaultValue);
                } else {
                    builder.nullVariable(getter.prop());
                }
                break;
            case NULL:
                builder.sql("null");
                break;
            case SEQUENCE:
                builder.sql("(")
                        .sql(
                                builder.sqlClient()
                                        .getDialect()
                                        .getSelectIdFromSequenceSql(sequenceIdGenerator.getSequenceName())
                        )
                        .sql(")");
                break;
            default:
                throw new AssertionError("Internal bug: Unexpected source value mode: " + mode);
        }
    }
}
