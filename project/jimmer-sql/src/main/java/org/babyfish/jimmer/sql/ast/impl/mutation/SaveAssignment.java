package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SaveAssignment {

    final PropertyGetter target;

    @Nullable
    final Expression<?> value;

    final List<PropertyGetter> inputGetters;

    private SaveAssignment(
            PropertyGetter target,
            @Nullable Expression<?> value,
            List<PropertyGetter> inputGetters
    ) {
        this.target = target;
        this.value = value;
        this.inputGetters = inputGetters;
    }

    static SaveAssignment custom(
            PropertyGetter target,
            Expression<?> value,
            List<PropertyGetter> inputGetters
    ) {
        return new SaveAssignment(
                target,
                value,
                Collections.unmodifiableList(new ArrayList<>(inputGetters))
        );
    }

    static SaveAssignment defaultOf(PropertyGetter getter) {
        return new SaveAssignment(getter, null, Collections.emptyList());
    }

    static List<SaveAssignment> defaults(List<PropertyGetter> getters) {
        List<SaveAssignment> assignments = new ArrayList<>(getters.size());
        for (PropertyGetter getter : getters) {
            assignments.add(defaultOf(getter));
        }
        return Collections.unmodifiableList(assignments);
    }
}
