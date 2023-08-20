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

    @Nullable
    private final String path;

    private List<AbstractProp> props;

    private List<DtoProp<T, P>> dtoProps;

    private List<UserProp> userProps;

    private List<DtoProp<T, P>> hiddenFlatProps;

    DtoType(
            T baseType,
            boolean isInput,
            @Nullable String name,
            @Nullable String path
    ) {
        this.baseType = baseType;
        this.isInput = isInput;
        this.name = name;
        this.path = path;
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

    @Nullable
    public String getPath() {
        return path;
    }

    public List<AbstractProp> getProps() {
        return props;
    }

    @SuppressWarnings("unchecked")
    public List<DtoProp<T, P>> getDtoProps() {
        List<DtoProp<T, P>> list = dtoProps;
        if (list == null) {
            list = new ArrayList<>();
            for (AbstractProp prop : props) {
                if (prop instanceof DtoProp<?, ?>) {
                    list.add((DtoProp<T, P>) prop);
                }
            }
            dtoProps = list;
        }
        return list;
    }

    public List<UserProp> getUserProps() {
        List<UserProp> list = userProps;
        if (list == null) {
            list = new ArrayList<>();
            for (AbstractProp prop : props) {
                if (prop instanceof UserProp) {
                    list.add((UserProp) prop);
                }
            }
            userProps = list;
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public List<DtoProp<T, P>> getHiddenFlatProps() {
        List<DtoProp<T, P>> hfps = this.hiddenFlatProps;
        if (hfps == null) {
            FlatDtoBuilder<T, P> builder = new FlatDtoBuilder<>(isInput, null);
            for (DtoProp<T, P> prop : getDtoProps()) {
                if (prop.getNextProp() != null) {
                    builder.add(prop);
                }
            }
            hfps = builder.build().getDtoProps();
            this.hiddenFlatProps = hfps;
        }
        return hfps;
    }

    void setProps(List<AbstractProp> props) {
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
        for (AbstractProp prop : props) {
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
            List<AbstractProp> props;
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
            DtoType<T, P> dtoType = new DtoType<>(null, isInput, null, null);
            dtoType.setProps(props);
            return dtoType;
        }
    }
}
