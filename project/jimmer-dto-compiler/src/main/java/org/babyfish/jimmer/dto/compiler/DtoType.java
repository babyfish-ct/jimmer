package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DtoType<T extends BaseType, P extends BaseProp> {

    private final T baseType;

    private final boolean isInput;

    @Nullable
    private final String name;

    private List<DtoProp<T, P>> props;

    DtoType(
            T baseType,
            boolean isInput,
            @Nullable String name
    ) {
        this.baseType = baseType;
        this.isInput = isInput;
        this.name = name;
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

    void setProps(List<DtoProp<T, P>> props) {
        if (props == null) {
            throw new IllegalArgumentException("`props` cannot be null");
        }
        if (this.props != null) {
            throw new IllegalArgumentException("`props` has already been set");
        }
        this.props = props;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (isInput) {
            builder.append("input ");
        }
        if (name != null) {
            builder.append(name).append(' ');
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
