package org.babyfish.jimmer.jackson.meta;

import com.fasterxml.jackson.databind.introspect.TypeResolutionContext;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.impl.util.StaticCache;

import java.util.Objects;

public class BeanProps {

    private static final StaticCache<Key, BeanProp> CACHE =
            new StaticCache<>(BeanProps::create);

    private BeanProps() {}

    public static BeanProp get(TypeFactory typeFactory, ImmutableProp prop) {
        return CACHE.get(new Key(typeFactory, prop));
    }

    private static BeanProp create(Key key) {
        return new BeanProp(new TypeResolutionContext.Empty(key.typeFactory), key.prop);
    }

    private static class Key {

        final TypeFactory typeFactory;

        final ImmutableProp prop;

        private Key(TypeFactory typeFactory, ImmutableProp prop) {
            this.typeFactory = typeFactory;
            this.prop = prop;
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeFactory, prop);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return typeFactory.equals(key.typeFactory) && prop == key.prop;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "typeFactory=" + typeFactory +
                    ", prop=" + prop +
                    '}';
        }
    }
}
