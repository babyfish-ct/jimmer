package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.lang.Generics;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Type;

public class JWeakJoinLambdaFactory extends AbstractWeakJoinLambdaFactory {

    private static final JWeakJoinLambdaFactory INSTANCE =
            new JWeakJoinLambdaFactory();

    public static WeakJoinLambda get(WeakJoin<?, ?> join) {
        return INSTANCE.getLambda(join);
    }

    @Override
    protected Class<?>[] getTypes(SerializedLambda serializedLambda) {
        org.babyfish.jimmer.impl.org.objectweb.asm.Type[] asmTypes =
                org.babyfish.jimmer.impl.org.objectweb.asm.Type.getArgumentTypes(serializedLambda.getImplMethodSignature());
        Class<?>[] types = new Class[asmTypes.length];
        for (int i = 0; i < asmTypes.length; i++) {
            String className = asmTypes[i].getInternalName().replace('/', '.');
            Class<?> type;
            try {
                type = Class.forName(className);
            } catch (ClassNotFoundException ex) {
                throw new IllegalArgumentException(
                        "Cannot pass the work join type \"" +
                                serializedLambda.getImplClass() +
                                "\", the type arguments[" +
                                i +
                                "] \"" +
                                className +
                                "\" cannot be found"
                );
            }
            if (BaseTable.class.isAssignableFrom(type)) {
                types[i] = type;
                continue;
            }
            if (!AbstractTypedTable.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException(
                        "Cannot pass the work join type \"" +
                                serializedLambda.getImplClass() +
                                "\", the type arguments[" +
                                i +
                                "] \"" +
                                className +
                                "\" must be derived class of \"" +
                                AbstractTypedTable.class.getName() +
                                "\""
                );
            }
            if (type.getTypeParameters().length != 0) {
                throw new IllegalArgumentException(
                        "Cannot pass the work join type \"" +
                                serializedLambda.getImplClass() +
                                "\", the type arguments[" +
                                i +
                                "] \"" +
                                className +
                                "\" cannot contains generic type parameter"
                );
            }
            Type[] arguments = Generics.getTypeArguments(type, Table.class);
            if (arguments.length == 0) {
                throw new IllegalArgumentException(
                        "Cannot pass the work join type \"" +
                                serializedLambda.getImplClass() +
                                "\", the type arguments[" +
                                i +
                                "] \"" +
                                className +
                                "\" must specify the generic parameter of \"" +
                                Table.class.getName() +
                                "\""
                );
            }
            Type argumentType = arguments[0];
            if (!(argumentType instanceof Class<?>)) {
                throw new IllegalArgumentException(
                        "Cannot pass the work join type \"" +
                                serializedLambda.getImplClass() +
                                "\", the type arguments[" +
                                i +
                                "] \"" +
                                className +
                                "\" must use class to specify the generic parameter of \"" +
                                Table.class.getName() +
                                "\""
                );
            }
            types[i] = (Class<?>) argumentType;
        }
        return types;
    }
}
