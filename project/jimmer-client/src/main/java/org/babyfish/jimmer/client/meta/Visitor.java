package org.babyfish.jimmer.client.meta;

import org.babyfish.jimmer.client.meta.impl.UnresolvedTypeParameterImpl;
import org.babyfish.jimmer.client.meta.impl.UnresolvedTypeVariableImpl;

public interface Visitor {

    default void visitingService(Service service) {}

    default void visitedService(Service service) {}

    default void visitingOperation(Operation operation) {}

    default void visitedOperation(Operation operation) {}

    default void visitParameter(Parameter parameter) {}


    default boolean isTypeVisitable(Type type) {
        return false; // Default value is false, because of circular reference
    }


    default void visitNullableType(NullableType nullableType) {}

    default void visitArrayType(ArrayType arrayType) {}

    default void visitMapType(MapType mapType) {}

    default void visitStaticObjectType(StaticObjectType staticObjectType) {}

    default void visitImmutableObjectType(ImmutableObjectType immutableObjectType) {}

    default void visitEnumType(EnumType enumType) {}

    default void visitSimpleType(SimpleType simpleType) {}

    default void visitUnresolvedTypeVariable(UnresolvedTypeVariableImpl unresolvedTypeVariable) {}

    default void visitUnresolvedTypeParameter(UnresolvedTypeParameterImpl unresolvedTypeParameter) {}
}
