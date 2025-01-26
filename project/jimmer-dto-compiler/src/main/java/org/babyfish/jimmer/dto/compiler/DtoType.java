package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DtoType<T extends BaseType, P extends BaseProp> {

    private final T baseType;

    private final String packageName;

    private final Set<DtoModifier> modifiers;

    private final List<Anno> annotations;

    private final List<TypeRef> superInterfaces;

    @Nullable
    private final String name;

    @NotNull
    private final DtoFile dtoFile;

    @Nullable
    private final String doc;

    private final boolean focusedRecursion;

    private List<AbstractProp> props;

    private List<DtoProp<T, P>> dtoProps;

    private List<UserProp> userProps;

    private List<DtoProp<T, P>> hiddenFlatProps;

    DtoType(
            T baseType,
            @Nullable String packageName,
            Set<DtoModifier> modifiers,
            List<Anno> annotations,
            List<TypeRef> superInterfaces,
            @Nullable String name,
            @NotNull DtoFile dtoFile,
            @Nullable String doc
    ) {
        this.baseType = baseType;
        this.packageName = packageName != null ? packageName : defaultPackageName(baseType.getPackageName());
        this.modifiers = modifiers;
        this.annotations = annotations;
        this.superInterfaces = superInterfaces;
        this.name = name;
        this.dtoFile = dtoFile;
        this.doc = doc;
        this.focusedRecursion = false;
    }

    @SuppressWarnings("unchecked")
    private DtoType(
            DtoType<T, P> original,
            DtoProp<T, P> recursionProp
    ) {
        this.baseType = original.baseType;
        this.packageName = original.packageName;
        this.modifiers = original.modifiers;
        this.annotations = original.annotations;
        this.superInterfaces = original.superInterfaces;
        this.name = null;
        this.dtoFile = original.dtoFile;
        this.doc = original.doc;
        this.focusedRecursion = true;
        List<AbstractProp> props = new ArrayList<>(original.props.size());
        for (AbstractProp prop : original.props) {
            if (prop instanceof DtoProp<?, ?>) {
                DtoProp<T, P> dtoProp = (DtoProp<T, P>) prop;
                if (dtoProp.isRecursive() && dtoProp != recursionProp) {
                    continue;
                }
            }
            props.add(prop);
        }
        this.props = Collections.unmodifiableList(props);
    }

    public T getBaseType() {
        return baseType;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getDoc() {
        return doc;
    }

    public Set<DtoModifier> getModifiers() {
        return modifiers;
    }

    public List<Anno> getAnnotations() {
        return annotations;
    }

    public List<TypeRef> getSuperInterfaces() {
        return superInterfaces;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public DtoFile getDtoFile() {
        return dtoFile;
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
            FlatDtoBuilder<T, P> builder = new FlatDtoBuilder<>(packageName, modifiers, null);
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

    DtoType<T, P> recursiveOne(DtoProp<T, P> recursionProp) {
        return new DtoType<>(this, recursionProp);
    }

    void setProps(List<AbstractProp> props) {
        if (props == null) {
            throw new IllegalArgumentException("`props` cannot be null");
        }
        if (this.props != null) {
            throw new IllegalArgumentException("`props` has already been set");
        }
        this.props = props;
        this.props = standardProps(props);
    }

    public boolean isFocusedRecursion() {
        return focusedRecursion;
    }

    @Override
    public String toString() {
        return toString(null);
    }

    String toString(@Nullable PropConfig config) {
        StringBuilder builder = new StringBuilder();
        if (doc != null) {
            builder.append("@doc(").append(doc.replace("\n", "\\n")).append(") ");
        }
        for (Anno anno : annotations) {
            builder.append(anno).append(' ');
        }
        for (DtoModifier modifier : modifiers) {
            builder.append(modifier.name().toLowerCase()).append(' ');
        }
        if (name != null) {
            builder.append(name).append(' ');
        }
        if (!superInterfaces.isEmpty()) {
            String separator = "implements ";
            for (TypeRef typeRef : superInterfaces) {
                builder.append(separator);
                separator = ", ";
                builder.append(typeRef);
            }
            builder.append(' ');
        }
        builder.append('{');
        boolean addComma = false;
        boolean isInput = modifiers.contains(DtoModifier.INPUT);
        if (config != null) {
            builder.append(config);
            addComma = true;
        }
        for (AbstractProp prop : props) {
            if (addComma) {
                builder.append(", ");
            } else {
                addComma = true;
            }
            if (isInput && prop.isNullable() && prop instanceof DtoProp<?, ?>) {
                builder.append(((DtoPropImpl<?, ?>) prop).toString(((DtoProp<?, ?>) prop).getInputModifier()));
            } else {
                builder.append(prop);
            }
        }
        builder.append('}');
        return builder.toString();
    }

    private class FlatDtoBuilder<T extends BaseType, P extends BaseProp> {

        private final String packageName;

        private final Set<DtoModifier> modifiers;

        private final DtoProp<T, P> prop;

        // Value: FlatDtoBuilder | DtoProp
        private final Map<String, Object> childNodes = new LinkedHashMap<>();

        FlatDtoBuilder(String packageName, Set<DtoModifier> modifiers, DtoProp<T, P> prop) {
            this.packageName = packageName;
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
                    child = new FlatDtoBuilder<>(packageName, modifiers, prop);
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
            DtoType<T, P> dtoType = new DtoType<>(
                    null,
                    packageName,
                    modifiers,
                    annotations,
                    Collections.emptyList(),
                    null,
                    dtoFile,
                    null
            );
            dtoType.setProps(props);
            return dtoType;
        }
    }

    private static String defaultPackageName(String entityPackageName) {
        if (entityPackageName == null || entityPackageName.isEmpty()) {
            return "dto";
        }
        return entityPackageName + ".dto";
    }

    @SuppressWarnings("unchecked")
    private static <T extends BaseType, P extends BaseProp> List<AbstractProp> standardProps(List<AbstractProp> props) {
        List<DtoProp<T, P>> recursiveProps = new ArrayList<>();
        for (AbstractProp prop : props) {
            if (prop instanceof DtoProp<?, ?>) {
                DtoProp<T, P> dtoProp = (DtoProp<T, P>) prop;
                if (dtoProp.isRecursive()) {
                    recursiveProps.add(dtoProp);
                }
            }
        }
        if (recursiveProps.size() <= 1) {
            return props;
        }
        List<AbstractProp> newProps = new ArrayList<>(props.size());
        for (AbstractProp prop : props) {
            if (prop instanceof DtoProp<?, ?>) {
                DtoProp<T, P> dtoProp = (DtoProp<T, P>) prop;
                if (dtoProp.isRecursive()) {
                    newProps.add(new DtoPropImpl<>(dtoProp));
                    continue;
                }
            }
            newProps.add(prop);
        }
        return newProps;
    }
}
