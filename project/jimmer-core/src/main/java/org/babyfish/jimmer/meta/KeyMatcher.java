package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.meta.impl.KeyMatcherImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface KeyMatcher {

    KeyMatcher EMPTY = new KeyMatcher() {

        @NotNull
        @Override
        public Map<String, Set<ImmutableProp>> toMap() {
            return Collections.emptyMap();
        }

        @NotNull
        @Override
        public List<ImmutableProp> getAllProps() {
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public Group match(Object entity) {
            return null;
        }

        @Override
        public Group match(Iterable<ImmutableProp> props) {
            return null;
        }

        @Override
        public List<ImmutableProp> missedProps(Iterable<ImmutableProp> props) {
            return Collections.emptyList();
        }
    };

    @NotNull
    Map<String, Set<ImmutableProp>> toMap();

    @NotNull
    List<ImmutableProp> getAllProps();

    @Nullable
    Group match(Object entity);

    Group match(Iterable<ImmutableProp> props);

    @NotNull
    default Set<ImmutableProp> matchedKeyProps(Object entity) {
        Group group = match(entity);
        if (group == null) {
            return Collections.emptySet();
        }
        return group.getProps();
    }

    @Nullable
    default Group getGroup(String name) {
        Set<ImmutableProp> props = toMap().get(name);
        if (props == null) {
            return null;
        }
        return new Group(name, props);
    }

    List<ImmutableProp> missedProps(Iterable<ImmutableProp> props);

    static KeyMatcher of(ImmutableType type, Map<String, Set<ImmutableProp>> map) {
        if (map.isEmpty()) {
            return EMPTY;
        }
        return new KeyMatcherImpl(type, map);
    }

    class Group {

        private final String name;

        private final Set<ImmutableProp> props;

        private final int hash;

        public Group(String name, Set<ImmutableProp> props) {
            this.name = name;
            this.props = props;
            this.hash = name.hashCode() ^ props.hashCode();
        }

        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        public Set<ImmutableProp> getProps() {
            return props;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Group that = (Group) o;

            if (!name.equals(that.name)) return false;
            return props.equals(that.props);
        }

        @Override
        public String toString() {
            return "KeyMatcher.Group{" +
                    "name='" + name + '\'' +
                    ", props=" + props +
                    "}";
        }
    }
}
