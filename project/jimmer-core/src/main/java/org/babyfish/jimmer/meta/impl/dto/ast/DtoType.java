package org.babyfish.jimmer.meta.impl.dto.ast;

import org.babyfish.jimmer.meta.impl.dto.ast.spi.BaseProp;
import org.babyfish.jimmer.meta.impl.dto.ast.spi.BaseType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DtoType<T extends BaseType, P extends BaseProp> {

    private final T baseType;

    private final boolean isInput;

    @Nullable
    private final String name;

    private final List<DtoProp<T, P>> props;

    DtoType(
            T baseType,
            boolean isInput,
            List<DtoProp<T, P>> props
    ) {
        this.baseType = baseType;
        this.isInput = isInput;
        this.name = null;
        this.props = props;
    }

    DtoType(DtoType<T, P> base, @NotNull String name) {
        this.baseType = base.baseType;
        this.isInput = base.isInput;
        this.name = name;
        this.props = base.props;
    }

    public T getBaseType() {
        return baseType;
    }

    public boolean isInput() {
        return isInput;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public List<DtoProp<T, P>> getProps() {
        return props;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (isInput) {
            builder.append("input");
        }
        if (name != null) {
            builder.append(' ').append(name);
        }
        builder.append('{');
        boolean addComma = false;
        for (DtoProp<T, P> prop : props) {
            if (addComma) {
                builder.append(", ");
            } else {
                addComma = true;
            }
            builder.append(prop);
        }
        builder.append('}');
        return builder.toString();
    }
}
