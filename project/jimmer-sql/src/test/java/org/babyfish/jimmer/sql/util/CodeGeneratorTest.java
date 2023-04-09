package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.BookTableEx;
import org.babyfish.jimmer.sql.model.microservice.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

public class CodeGeneratorTest {

    @Test
    public void testTable() {

        generated(BookTable.class, "name");
        notGenerated(BookTableEx.class, "name");

        generated(BookTable.class, "store");
        generated(BookTable.class, "store", JoinType.class);
        generated(BookTableEx.class, "store");
        generated(BookTableEx.class, "store", JoinType.class);

        notGenerated(BookTable.class, "authors");
        notGenerated(BookTable.class, "authors", JoinType.class);
        generated(BookTableEx.class, "authors");
        generated(BookTableEx.class, "authors", JoinType.class);

        notGenerated(OrderTable.class, "orderItems");
        notGenerated(OrderTable.class, "orderItems", JoinType.class);
        notGenerated(OrderTableEx.class, "orderItems");
        notGenerated(OrderTableEx.class, "orderItems", JoinType.class);

        generated(OrderItemTable.class, "order");
        generated(OrderItemTable.class, "order", JoinType.class);
        notGenerated(OrderItemTableEx.class, "order");
        notGenerated(OrderItemTableEx.class, "order", JoinType.class);

        notGenerated(OrderItemTable.class, "products");
        notGenerated(OrderItemTable.class, "products", JoinType.class);
        generated(OrderItemTableEx.class, "products");
        generated(OrderItemTableEx.class, "products", JoinType.class);

        notGenerated(ProductTable.class, "orderItems");
        notGenerated(ProductTable.class, "orderItems", JoinType.class);
        notGenerated(ProductTableEx.class, "orderItems");
        notGenerated(ProductTableEx.class, "orderItems", JoinType.class);
    }

    public static void generated(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            type.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException ex) {
            Assertions.fail(
                    "There is no method \"" +
                            name +
                            "\" with parameters " +
                            Arrays.toString(parameterTypes) +
                            " in type \"" +
                            type.getName() +
                            "\""
            );
        }
    }

    public static void notGenerated(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            Method method = type.getDeclaredMethod(name, parameterTypes);
            Assertions.fail(
                    "The method \"" +
                            method +
                            "\" should not be generated"
            );
        } catch (NoSuchMethodException ex) {

        }
    }
}
