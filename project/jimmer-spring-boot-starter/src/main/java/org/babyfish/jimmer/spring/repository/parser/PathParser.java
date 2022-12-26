package org.babyfish.jimmer.spring.repository.parser;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class PathParser {

    private final Context ctx;

    private final boolean allowCollection;

    private final List<ImmutableProp> props;

    PathParser(Context ctx, boolean allowCollection) {
        this.ctx = ctx;
        this.allowCollection = allowCollection;
        this.props = new ArrayList<>();
    }

    public Path parse(Source source, ImmutableType type) {
        if (!parse0(source, type)) {
            throw new IllegalArgumentException(
                    "Cannot resolve the property name \"" +
                            source +
                            "\" by \"" +
                            type +
                            "\""
            );
        }
        return new Path(source, Collections.unmodifiableList(props));
    }

    private boolean parse0(Source source, ImmutableType type) {
        List<ImmutableProp> props = ctx.getOrderedProps(type);
        for (ImmutableProp prop : props) {
            if (!prop.isReferenceList(TargetLevel.ENTITY) || allowCollection) {
                if (parse0(source, prop)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean parse0(Source source, ImmutableProp prop) {
        Source restSource = trimByProp(source, prop);
        if (restSource == null) {
            return false;
        }
        props.add(prop);
        if (!restSource.isEmpty() && (prop.getTargetType() == null)) {
            throw new IllegalArgumentException(
                    "Cannot resolve the property name \"" +
                            source +
                            "\" by \"" +
                            prop.getDeclaringType() +
                            "\""
            );
        }
        if (!restSource.isEmpty()) {
            return parse0(restSource, prop.getTargetType());
        }
        return true;
    }

    private static Source trimByProp(Source source, ImmutableProp prop) {
        String name = prop.getName();
        int len = name.length();
        if (source.length() < len) {
            return null;
        }
        boolean toLowerCase = true;
        for (int i = 0; i < len; i++) {
            char ch = source.charAt(i);
            char expectedCh = name.charAt(i);
            boolean matched =
                    toLowerCase ?
                            Character.toLowerCase(ch) == expectedCh :
                            ch == expectedCh;
            if (!matched) {
                return null;
            }
            if (Character.isLowerCase(ch)) {
                toLowerCase = false;
            }
        }
        return source.subSource(len, source.length());
    }
}
