package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;

import java.util.*;

public class UpsertMaskPath {

    private final List<ImmutableProp> props;

    private UpsertMaskPath(List<ImmutableProp> props) {
        this.props = props;
    }

    public List<ImmutableProp> getProps() {
        return props;
    }

    public static UpsertMaskPath get(TypedProp.Single<?, ?> ... props) {
        ImmutableProp[] unwrappedProps = new ImmutableProp[props.length];
        for (int i = props.length - 1; i >= 0; --i) {
            unwrappedProps[i] = props[i].unwrap();
        }
        return get(unwrappedProps);
    }

    public static UpsertMaskPath get(ImmutableProp ... props) {
        int len = props.length;
        if (len == 0) {
            throw new IllegalArgumentException("UpsertMaskPath cannot be empty");
        }
        ImmutableProp prop = props[0];
        if (prop == null) {
            throw new IllegalArgumentException("The props[0] cannot be null");
        }
        if (!prop.getDeclaringType().isEntity()) {
            throw new IllegalArgumentException(
                    "The props[0] \"" +
                            prop +
                            "\" of the UpsertMaskPath be declared in entity"
            );
        }
        if (!prop.isColumnDefinition()) {
            throw new IllegalArgumentException(
                    "The first property \"" +
                            prop +
                            "\" is not based on database column"
            );
        }
        for (int i = 1; i < len; i++) {
            ImmutableProp p = props[i];
            if (p == null) {
                throw new IllegalArgumentException("The props[" + i + "] cannot be null");
            }
            if (!prop.isEmbedded(EmbeddedLevel.BOTH)) {
                throw new IllegalArgumentException(
                        "The type of property \"" +
                                prop +
                                "\" is not embeddable"
                );
            }
            ImmutableType type = prop.getTargetType();
            if (type.isEntity()) {
                type = type.getIdProp().getTargetType();
            }
            if (p.getDeclaringType() != type) {
                throw new IllegalArgumentException(
                        "The props[" +
                                i +
                                "] \"" +
                                prop +
                                "\" is must be declared in \"" +
                                type +
                                "\""
                );
            }
            if (!p.isColumnDefinition()) {
                throw new IllegalArgumentException(
                        "The props[" +
                                i +
                                "] \"" +
                                prop +
                                "\" is not columns of embeddable type"
                );
            }
            prop = p;
        }
        List<ImmutableProp> list = new ArrayList<>(Arrays.asList(props));
        return new UpsertMaskPath(Collections.unmodifiableList(list));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Iterator<ImmutableProp> itr = props.iterator();
        itr.next();
        builder.append(itr.next());
        while (itr.hasNext()) {
            builder.append('.').append(itr.next().getName());
        }
        return builder.toString();
    }
}
