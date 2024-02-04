package org.babyfish.jimmer.sql.event.binlog.impl;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.runtime.ImmutableSpi;

public class MiddleRow<S, T> {

    public final S sourceId;

    public final T targetId;

    public final Boolean deleted;

    public final Object filteredValue;

    public MiddleRow(S sourceId, T targetId, Boolean deleted, Object filteredValue) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.deleted = deleted;
        this.filteredValue = filteredValue;
    }

    public static <S, T> MiddleRow<S, T> merge(MiddleRow<S, T> oldRow, MiddleRow<S, T> newRow) {
        if (oldRow == null) {
            return newRow;
        }
        if (newRow == null) {
            return null;
        }
        if (newRow.sourceId != null && newRow.targetId != null && newRow.deleted != null &&
                (oldRow.filteredValue == null || newRow.filteredValue != null)
        ) {
            return newRow;
        }
        S sourceId = newRow.sourceId;
        if (sourceId == null) {
            sourceId = oldRow.sourceId;
        } else if (sourceId instanceof ImmutableSpi) {
            sourceId = ImmutableObjects.merge(oldRow.sourceId, sourceId);
        }
        T targetId = newRow.targetId;
        if (targetId == null) {
            targetId = oldRow.targetId;
        } else if (targetId instanceof ImmutableSpi) {
            targetId = ImmutableObjects.merge(oldRow.targetId, targetId);
        }
        return new MiddleRow<>(
                sourceId,
                targetId,
                newRow.deleted != null ? newRow.deleted : oldRow.deleted,
                newRow.filteredValue != null ? newRow.filteredValue : oldRow.filteredValue
        );
    }

    @Override
    public String toString() {
        return "MiddleRow{" +
                "sourceId=" + sourceId +
                ", targetId=" + targetId +
                ", deleted=" + deleted +
                ", filteredValue=" + filteredValue +
                '}';
    }
}
