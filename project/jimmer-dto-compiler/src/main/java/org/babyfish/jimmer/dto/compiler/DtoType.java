package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DtoType<T extends BaseType, P extends BaseProp> {

    private final T baseType;

    private final boolean isInput;

    @Nullable
    private final String name;

    private List<DtoProp<T, P>> props;

    private List<DtoProp<T, P>> hiddenFlatProps;

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

    public List<DtoProp<T, P>> getHiddenFlatProps() {
        List<DtoProp<T, P>> hfps = this.hiddenFlatProps;
        if (hfps == null) {
            FlatDtoBuilder<T, P> builder = new FlatDtoBuilder<>(isInput, null);
            for (DtoProp<T, P> prop : props) {
                if (prop.getNextProp() != null) {
                    builder.add(prop);
                }
            }
            hfps = builder.build().getProps();
            this.hiddenFlatProps = hfps;
        }
        return hfps;
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

    private class FlatDtoBuilder<T extends BaseType, P extends BaseProp> {

        private final boolean isInput;

        private final DtoProp<T, P> prop;

        // Value: FlatDtoBuilder | DtoProp
        private final Map<String, Object> childNodes = new LinkedHashMap<>();

        FlatDtoBuilder(boolean isInput, DtoProp<T, P> prop) {
            this.isInput = isInput;
            this.prop = prop;
        }

        @SuppressWarnings("unchecked")
        public void add(DtoProp<T, P> prop) {
            String baseName = prop.getBaseProp().getName();
            if (prop.getNextProp() == null) {
                childNodes.put(baseName, prop);
            } else {
                FlatDtoBuilder<T, P> child = (FlatDtoBuilder<T, P>) childNodes.get(baseName);
                if (child == null) {
                    child = new FlatDtoBuilder<>(isInput, prop);
                    childNodes.put(baseName, child);
                }
                child.add(prop.getNextProp());
            }
        }

        @SuppressWarnings("unchecked")
        public DtoType<T, P> build() {
            List<DtoProp<T, P>> props;
            if (childNodes.isEmpty()) {
                props = Collections.emptyList();
            } else {
                props = new ArrayList<>();
                for (Object child : childNodes.values()) {
                    if (child instanceof DtoProp<?, ?>) {
                        props.add((DtoProp<T, P>) child);
                    } else {
                        FlatDtoBuilder<T, P> builder = (FlatDtoBuilder<T, P>) child;
                        DtoProp<T, P> prop = new DtoPropImpl<>(
                                builder.prop,
                                builder.build()
                        );
                        props.add(prop);
                    }
                }
                props = Collections.unmodifiableList(props);
            }
            DtoType<T, P> dtoType = new DtoType<>(null, isInput, null);
            dtoType.setProps(props);
            return dtoType;
        }
    }
}
