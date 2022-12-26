package org.babyfish.jimmer.spring.repository.parser;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;

import java.util.List;
import java.util.stream.Collectors;

public class Path {

    private final Source source;

    private final List<ImmutableProp> props;

    public static Path of(Context ctx, boolean allowCollection, Source source, ImmutableType type) {
        return new PathParser(ctx, allowCollection).parse(source, type);
    }

    public Path(Source source, List<ImmutableProp> props) {
        this.source = source;
        this.props = props;
    }

    public Source getSource() {
        return source;
    }

    public List<ImmutableProp> getProps() {
        return props;
    }

    public boolean isScalar() {
        return props.get(props.size() - 1).isScalar(TargetLevel.ENTITY);
    }

    public Class<?> getType() {
        return props.get(props.size() - 1).getElementClass();
    }

    @Override
    public String toString() {
        return props.stream().map(ImmutableProp::getName).collect(Collectors.joining("."));
    }
}
