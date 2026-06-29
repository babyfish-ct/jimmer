package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class DtoPolymorphism<T extends BaseType, P extends BaseProp> {

    private final boolean exhaustive;

    @Nullable
    private final DtoPolymorphicBranch<T, P> defaultBranch;

    private final List<DtoPolymorphicBranch<T, P>> subtypeBranches;

    DtoPolymorphism(
            boolean exhaustive,
            @Nullable DtoPolymorphicBranch<T, P> defaultBranch,
            List<DtoPolymorphicBranch<T, P>> subtypeBranches
    ) {
        this.exhaustive = exhaustive;
        this.defaultBranch = defaultBranch;
        this.subtypeBranches = Collections.unmodifiableList(subtypeBranches);
    }

    public boolean isExhaustive() {
        return exhaustive;
    }

    @Nullable
    public DtoPolymorphicBranch<T, P> getDefaultBranch() {
        return defaultBranch;
    }

    public List<DtoPolymorphicBranch<T, P>> getSubtypeBranches() {
        return subtypeBranches;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("#subtypes {");
        boolean addComma = false;
        if (exhaustive) {
            builder.append("#exhaustive");
            addComma = true;
        }
        if (defaultBranch != null) {
            if (addComma) {
                builder.append(", ");
            } else {
                addComma = true;
            }
            builder.append(defaultBranch);
        }
        for (DtoPolymorphicBranch<T, P> branch : subtypeBranches) {
            if (addComma) {
                builder.append(", ");
            } else {
                addComma = true;
            }
            builder.append(branch);
        }
        builder.append('}');
        return builder.toString();
    }
}
