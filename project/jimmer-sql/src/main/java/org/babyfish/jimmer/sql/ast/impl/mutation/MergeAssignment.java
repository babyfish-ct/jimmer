package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class MergeAssignment {

    final PropertyGetter target;

    private MergeAssignment(PropertyGetter target) {
        this.target = target;
    }

    static List<MergeAssignment> defaults(List<PropertyGetter> getters) {
        List<MergeAssignment> assignments = new ArrayList<>(getters.size());
        for (PropertyGetter getter : getters) {
            assignments.add(new MergeAssignment(getter));
        }
        return Collections.unmodifiableList(assignments);
    }
}
