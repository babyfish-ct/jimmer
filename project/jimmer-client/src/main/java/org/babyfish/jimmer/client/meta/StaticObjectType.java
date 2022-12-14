package org.babyfish.jimmer.client.meta;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public interface StaticObjectType extends ObjectType {

    List<Type> getTypeArguments();

    @Override
    default boolean hasDefinition() {
        return getTypeArguments().isEmpty();
    }

    class Key {

        private final Class<?> javaType;

        private final List<Type> typeArguments;

        public Key(Class<?> javaType, List<Type> typeArguments) {
            this.javaType = javaType;
            this.typeArguments = typeArguments != null ? typeArguments : Collections.emptyList();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key that = (Key) o;
            return Objects.equals(javaType, that.javaType) && Objects.equals(typeArguments, that.typeArguments);
        }

        @Override
        public int hashCode() {
            return Objects.hash(javaType, typeArguments);
        }

        @Override
        public String toString() {
            return "StaticObject.Key{" +
                    "javaType=" + javaType +
                    ", typeArguments=" + typeArguments +
                    '}';
        }
    }
}
