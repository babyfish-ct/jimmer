package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.Objects;

public interface AffectedTable {

    static Entity of(Class<?> javaType) {
        return of(ImmutableType.get(javaType));
    }

    static Entity of(ImmutableType type) {
        if (!(type.getImmutableAnnotation() instanceof org.babyfish.jimmer.sql.Entity)) {
            throw new IllegalArgumentException(
                    "\"" +
                            type +
                            "\" is not entity"
            );
        }
        return new Entity(type);
    }

    static <T extends Table<?>> Middle of(TypedProp.Association<?, ?> prop) {
        return new Middle(prop.unwrap());
    }

    static Middle of(ImmutableProp prop) {
        return new Middle(prop);
    }

    final class Entity implements AffectedTable {

        private ImmutableType type;

        Entity(ImmutableType type) {
            this.type = Objects.requireNonNull(type, "type cannot be null");
        }

        public ImmutableType getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return type.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entity entity = (Entity) o;
            return type.equals(entity.type);
        }

        @Override
        public String toString() {
            return type.getTableName() + '(' + type + ')';
        }
    }

    final class Middle implements AffectedTable {

        private ImmutableProp prop;

        Middle(ImmutableProp prop) {
            Objects.requireNonNull(prop, "prop cannot be null");
            ImmutableProp storageProp = prop.getMappedBy() != null ?
                    prop.getMappedBy() :
                    prop;
            if (!(storageProp.getStorage() instanceof MiddleTable)) {
                throw new IllegalArgumentException(
                        "\"" + prop + "\" is neither middle table property nor inverse property of middle table property"
                );
            }
            this.prop = storageProp;
        }

        public ImmutableProp getProp() {
            return prop;
        }

        @Override
        public int hashCode() {
            return prop.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Middle middle = (Middle) o;
            return prop.equals(middle.prop);
        }

        @Override
        public String toString() {
            return prop.<MiddleTable>getStorage().getTableName() + '(' + prop + ')';
        }
    }
}
