package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DtoType<T extends BaseType, P extends BaseProp> {

    private final T baseType;

    private final List<Anno> annotations;

    private final Set<DtoTypeModifier> modifiers;

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
            List<Anno> annotations,
            Set<DtoTypeModifier> modifiers,
            @Nullable String name,
            @Nullable String path
    ) {
        this.baseType = baseType;
        this.annotations = annotations;
        this.modifiers = modifiers;
        this.name = name;
        this.path = path;
    }

    public T getBaseType() {
        return baseType;
    }

    public Set<DtoTypeModifier> getModifiers() {
        return modifiers;
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
            FlatDtoBuilder<T, P> builder = new FlatDtoBuilder<>(modifiers, null);
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

    public List<Anno> getAnnotations() {
        return annotations;
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
        for (Anno anno : annotations) {
            builder.append(anno).append(' ');
        }
        for (DtoTypeModifier modifier : modifiers) {
            builder.append(modifier.name().toLowerCase()).append(' ');
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

        private final Set<DtoTypeModifier> modifiers;

        private final DtoProp<T, P> prop;

        // Value: FlatDtoBuilder | DtoProp
        private final Map<String, Object> childNodes = new LinkedHashMap<>();

        FlatDtoBuilder(Set<DtoTypeModifier> modifiers, DtoProp<T, P> prop) {
            this.modifiers = modifiers;
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
                    child = new FlatDtoBuilder<>(modifiers, prop);
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
            DtoType<T, P> dtoType = new DtoType<>(null, annotations, modifiers, null, null);
            dtoType.setProps(props);
            return dtoType;
        }
    }
}
