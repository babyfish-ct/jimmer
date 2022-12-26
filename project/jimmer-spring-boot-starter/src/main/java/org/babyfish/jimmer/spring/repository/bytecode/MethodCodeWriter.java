package org.babyfish.jimmer.spring.repository.bytecode;

import org.babyfish.jimmer.impl.asm.MethodVisitor;
import org.babyfish.jimmer.impl.asm.Opcodes;
import org.babyfish.jimmer.impl.asm.Type;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.spring.repository.parser.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class MethodCodeWriter implements Constants {

    protected final ClassCodeWriter parent;

    protected final Method method;

    protected final String id;

    protected MethodVisitor mv;

    private Method defaultImplMethod;

    private boolean defaultImplMethodCreated;

    protected MethodCodeWriter(ClassCodeWriter parent, Method method, String id) {
        this.parent = parent;
        this.method = method;
        this.id = id;
    }

    public void write() {
        if (method.isDefault()) {
            return;
        }
        QueryMethod queryMethod = QueryMethod.of(
                parent.ctx,
                ImmutableType.get(parent.metadata.getDomainType()),
                method
        );
        List<Integer> slots = new ArrayList<>();
        int slot = 0;
        for (Class<?> type : method.getParameterTypes()) {
            slots.add(++slot);
            if (type == long.class || type == double.class) {
                ++slot;
            }
        }
        mv = parent.getClassVisitor().visitMethod(
                Opcodes.ACC_PUBLIC,
                method.getName(),
                Type.getMethodDescriptor(method),
                null,
                null
        );
        mv.visitCode();
        writeCode(queryMethod, slots);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void writeCode(QueryMethod queryMethod, List<Integer> slots) {

        visitLoadJSqlClient();

        mv.visitLdcInsn(Type.getType(parent.metadata.getDomainType()));
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                IMMUTABLE_TYPE_INTERNAL_NAME,
                "get",
                "(Ljava/lang/Class;)" + IMMUTABLE_TYPE_DESCRIPTOR,
                true
        );

        mv.visitFieldInsn(
                Opcodes.GETSTATIC,
                parent.getImplInternalName(),
                queryMethodFieldName(),
                QUERY_METHOD_DESCRIPTOR
        );

        if (queryMethod.getPageableParamIndex() != -1) {
            mv.visitVarInsn(Opcodes.ALOAD, slots.get(queryMethod.getPageableParamIndex()));
        } else {
            mv.visitInsn(Opcodes.ACONST_NULL);
        }

        if (queryMethod.getSortParamIndex() != -1) {
            mv.visitVarInsn(Opcodes.ALOAD, slots.get(queryMethod.getSortParamIndex()));
        } else {
            mv.visitInsn(Opcodes.ACONST_NULL);
        }

        if (queryMethod.getFetcherIndex() != -1) {
            mv.visitVarInsn(Opcodes.ALOAD, slots.get(queryMethod.getFetcherIndex()));
        } else {
            mv.visitInsn(Opcodes.ACONST_NULL);
        }

        List<PropPredicate> ps = new ArrayList<>();
        collectPropPredicate(queryMethod.getQuery().getPredicate(), ps);
        mv.visitLdcInsn(argCount(ps));
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (PropPredicate p : ps) {
            if (p.getLogicParamIndex() != -1) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitLdcInsn(p.getLogicParamIndex());
                visitLoad(parameterTypes[p.getParamIndex()], slots.get(p.getParamIndex()));
                visitBox(parameterTypes[p.getParamIndex()]);
                mv.visitInsn(Opcodes.AASTORE);
            }
            if (p.getLogicParamIndex2() != -1) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitLdcInsn(p.getLogicParamIndex2());
                visitLoad(parameterTypes[p.getParamIndex2()], slots.get(p.getParamIndex2()));
                visitBox(parameterTypes[p.getParamIndex2()]);
                mv.visitInsn(Opcodes.AASTORE);
            }
        }
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                QUERY_EXECUTORS_INTERNAL_NAME,
                "execute",
                QUERY_EXECUTORS_METHOD_DESCRIPTOR,
                false
        );
        visitUnbox(method.getReturnType(), true);
        if (method.getReturnType() == void.class) {
            mv.visitInsn(Opcodes.POP);
        }
        visitReturn(method.getReturnType());
    }

    private static void collectPropPredicate(Predicate predicate, List<PropPredicate> propPredicates) {
        if (predicate instanceof PropPredicate) {
            propPredicates.add((PropPredicate) predicate);
        } else if (predicate instanceof AndPredicate) {
            for (Predicate subPredicate : ((AndPredicate)predicate).getPredicates()) {
                collectPropPredicate(subPredicate, propPredicates);
            }
        } else if (predicate instanceof OrPredicate) {
            for (Predicate subPredicate : ((OrPredicate)predicate).getPredicates()) {
                collectPropPredicate(subPredicate, propPredicates);
            }
        }
    }

    private static int argCount(List<PropPredicate> propPredicates) {
        int count = 0;
        for (PropPredicate p : propPredicates) {
            if (p.getLogicParamIndex() != -1) {
                count++;
            }
            if (p.getLogicParamIndex2() != -1) {
                count++;
            }
        }
        return count;
    }

    protected abstract void visitLoadJSqlClient();

    public MethodVisitor getMethodVisitor() {
        return mv;
    }

    String queryMethodFieldName() {
        return "QUERY_METHOD_{" + id + '}';
    }

    protected final Method getDefaultImplMethod() {
        if (defaultImplMethodCreated) {
            return defaultImplMethod;
        }
        defaultImplMethod = onGetDefaultImplMethod();
        defaultImplMethodCreated = true;
        return defaultImplMethod;
    }

    protected Method onGetDefaultImplMethod() {
        return null;
    }

    protected void visitReturn(Class<?> type) {
        if (type == void.class) {
            mv.visitInsn(Opcodes.RETURN);
        } else if (type == boolean.class) {
            mv.visitInsn(Opcodes.IRETURN);
        } else if (type == char.class) {
            mv.visitInsn(Opcodes.IRETURN);
        } else if (type == byte.class) {
            mv.visitInsn(Opcodes.IRETURN);
        } else if (type == short.class) {
            mv.visitInsn(Opcodes.IRETURN);
        } else if (type == int.class) {
            mv.visitInsn(Opcodes.IRETURN);
        } else if (type == long.class) {
            mv.visitInsn(Opcodes.LRETURN);
        } else if (type == float.class) {
            mv.visitInsn(Opcodes.FRETURN);
        } else if (type == double.class) {
            mv.visitInsn(Opcodes.DRETURN);
        } else {
            mv.visitInsn(Opcodes.ARETURN);
        }
    }

    private int visitLoad(Class<?> type, int slot) {
        if (type == boolean.class) {
            mv.visitVarInsn(Opcodes.ILOAD, slot);
            return 1;
        }
        if (type == char.class) {
            mv.visitVarInsn(Opcodes.ILOAD, slot);
            return 1;
        }
        if (type == byte.class) {
            mv.visitVarInsn(Opcodes.ILOAD, slot);
            return 1;
        }
        if (type == short.class) {
            mv.visitVarInsn(Opcodes.ILOAD, slot);
            return 1;
        }
        if (type == int.class) {
            mv.visitVarInsn(Opcodes.ILOAD, slot);
            return 1;
        }
        if (type == long.class) {
            mv.visitVarInsn(Opcodes.LLOAD, slot);
            return 2;
        }
        if (type == float.class) {
            mv.visitVarInsn(Opcodes.FLOAD, slot);
            return 1;
        }
        if (type == double.class) {
            mv.visitVarInsn(Opcodes.DLOAD, slot);
            return 2;
        }
        mv.visitVarInsn(Opcodes.ALOAD, slot);
        return 1;
    }

    private void visitBox(Class<?> type) {
        if (type == boolean.class) {
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Boolean",
                    "valueOf",
                    "(Z)Ljava/lang/Boolean;",
                    false
            );
        } else if (type == char.class) {
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Character",
                    "valueOf",
                    "(C)Ljava/lang/Character;",
                    false
            );
        } else if (type == byte.class) {
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Byte",
                    "valueOf",
                    "(B)Ljava/lang/Byte;",
                    false
            );
        } else if (type == short.class) {
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Short",
                    "valueOf",
                    "(S)Ljava/lang/Short;",
                    false
            );
        } else if (type == int.class) {
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Integer",
                    "valueOf",
                    "(I)Ljava/lang/Integer;",
                    false
            );
        } else if (type == long.class) {
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Long",
                    "valueOf",
                    "(J)Ljava/lang/Long;",
                    false
            );
        } else if (type == float.class) {
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Float",
                    "valueOf",
                    "(F)Ljava/lang/Float;",
                    false
            );
        } else if (type == double.class) {
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Double",
                    "valueOf",
                    "(D)Ljava/lang/Double;",
                    false
            );
        }
    }

    private void visitUnbox(Class<?> type, boolean autoCast) {
        if (type == boolean.class) {
            if (autoCast) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
            }
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Boolean",
                    "booleanValue",
                    "()Z",
                    false
            );
        } else if (type == char.class) {
            if (autoCast) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
            }
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Character",
                    "charValue",
                    "()C",
                    false
            );
        } else if (type == byte.class) {
            if (autoCast) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte");
            }
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Byte",
                    "byteValue",
                    "()B",
                    false
            );
        } else if (type == short.class) {
            if (autoCast) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");
            }
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Short",
                    "shortValue",
                    "()S",
                    false
            );
        } else if (type == int.class) {
            if (autoCast) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
            }
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Integer",
                    "intValue",
                    "()I",
                    false
            );
        } else if (type == long.class) {
            if (autoCast) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
            }
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Long",
                    "longValue",
                    "()J",
                    false
            );
        } else if (type == float.class) {
            if (autoCast) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
            }
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Float",
                    "floatValue",
                    "()F",
                    false
            );
        } else if (type == double.class) {
            if (autoCast) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
            }
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Double",
                    "doubleValue",
                    "()D",
                    false
            );
        } else {
            if (autoCast) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(type));
            }
        }
    }

    protected class VarLoader {

        private int slot;

        public VarLoader(int slot) {
            this.slot = slot;
        }

        public void load(Class<?> type) {
            this.slot += visitLoad(type, slot);
        }
    }
}
