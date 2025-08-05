package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.impl.org.objectweb.asm.Handle;
import org.babyfish.jimmer.impl.org.objectweb.asm.tree.*;

import java.util.*;

class InsnListUtils {

    private InsnListUtils() {}

    public static void eraseLambdaMagicNumber(InsnList list) {
        for (AbstractInsnNode insn : list) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                methodInsn.owner = eraseLambdaMagicNumber(methodInsn.owner);
                methodInsn.name = eraseLambdaMagicNumber(methodInsn.name);
                methodInsn.desc = eraseLambdaMagicNumber(methodInsn.desc);
            } else if (insn instanceof FieldInsnNode) {
                FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                fieldInsn.owner = eraseLambdaMagicNumber(fieldInsn.owner);
                fieldInsn.name = eraseLambdaMagicNumber(fieldInsn.name);
                fieldInsn.desc = eraseLambdaMagicNumber(fieldInsn.desc);
            } else if (insn instanceof InvokeDynamicInsnNode) {
                InvokeDynamicInsnNode indyInsn = (InvokeDynamicInsnNode) insn;
                for (int i = 0; i < indyInsn.bsmArgs.length; i++) {
                    Object arg = indyInsn.bsmArgs[i];
                    if (arg instanceof Handle) {
                        Handle handle = (Handle) arg;
                        indyInsn.bsmArgs[i] = new Handle(
                                handle.getTag(),
                                eraseLambdaMagicNumber(handle.getOwner()),
                                eraseLambdaMagicNumber(handle.getName()),
                                eraseLambdaMagicNumber(handle.getDesc()),
                                handle.isInterface()
                        );
                    }
                }
                indyInsn.desc = eraseLambdaMagicNumber(indyInsn.desc);
            } else if (insn instanceof TypeInsnNode) {
                TypeInsnNode typeInsn = (TypeInsnNode) insn;
                typeInsn.desc = eraseLambdaMagicNumber(typeInsn.desc);
            } else if (insn instanceof MultiANewArrayInsnNode) {
                MultiANewArrayInsnNode arrayInsn = (MultiANewArrayInsnNode) insn;
                arrayInsn.desc = eraseLambdaMagicNumber(arrayInsn.desc);
            }
        }
    }

    private static String eraseLambdaMagicNumber(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\$\\d+", "");
    }

    public static int hashCode(InsnList list) {
        if (list == null) {
            return 0;
        }

        int result = 1;
        Map<LabelNode, Integer> labelPositions = buildLabelPositionMap(list);

        for (int i = 0; i < list.size(); i++) {
            AbstractInsnNode insn = list.get(i);
            result = 31 * result + insnHashCode(insn, labelPositions);
        }

        return result;
    }

    public static boolean equals(InsnList list1, InsnList list2) {
        if (list1 == list2) {
            return true;
        }
        if (list1 == null || list2 == null) {
            return false;
        }
        if (list1.size() != list2.size()) {
            return false;
        }

        Map<LabelNode, Integer> labelPositions1 = buildLabelPositionMap(list1);
        Map<LabelNode, Integer> labelPositions2 = buildLabelPositionMap(list2);

        for (int i = 0; i < list1.size(); i++) {
            AbstractInsnNode insn1 = list1.get(i);
            AbstractInsnNode insn2 = list2.get(i);
            if (!compareInsnNodes(insn1, insn2, labelPositions1, labelPositions2)) {
                return false;
            }
        }

        return true;
    }

    private static Map<LabelNode, Integer> buildLabelPositionMap(InsnList list) {
        Map<LabelNode, Integer> map = new IdentityHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            AbstractInsnNode insn = list.get(i);
            if (insn instanceof LabelNode) {
                map.put((LabelNode) insn, i);
            }
        }
        return map;
    }

    private static boolean compareInsnNodes(
            AbstractInsnNode n1,
            AbstractInsnNode n2,
            Map<LabelNode, Integer> positions1,
            Map<LabelNode, Integer> positions2
    ) {
        if (n1.getOpcode() != n2.getOpcode()) return false;
        if (n1.getType() != n2.getType()) return false;

        switch (n1.getType()) {
            case AbstractInsnNode.INSN:
                return true;

            case AbstractInsnNode.INT_INSN:
                return ((IntInsnNode) n1).operand == ((IntInsnNode) n2).operand;

            case AbstractInsnNode.VAR_INSN:
                return ((VarInsnNode) n1).var == ((VarInsnNode) n2).var;

            case AbstractInsnNode.TYPE_INSN:
                return Objects.equals(((TypeInsnNode) n1).desc, ((TypeInsnNode) n2).desc);

            case AbstractInsnNode.FIELD_INSN:
                FieldInsnNode f1 = (FieldInsnNode) n1;
                FieldInsnNode f2 = (FieldInsnNode) n2;
                return Objects.equals(f1.owner, f2.owner) &&
                        Objects.equals(f1.name, f2.name) &&
                        Objects.equals(f1.desc, f2.desc);

            case AbstractInsnNode.METHOD_INSN:
                MethodInsnNode m1 = (MethodInsnNode) n1;
                MethodInsnNode m2 = (MethodInsnNode) n2;
                return Objects.equals(m1.owner, m2.owner) &&
                        Objects.equals(m1.name, m2.name) &&
                        Objects.equals(m1.desc, m2.desc);

            case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
                InvokeDynamicInsnNode id1 = (InvokeDynamicInsnNode) n1;
                InvokeDynamicInsnNode id2 = (InvokeDynamicInsnNode) n2;
                return Objects.equals(id1.name, id2.name) &&
                        Objects.equals(id1.desc, id2.desc) &&
                        Objects.equals(id1.bsm, id2.bsm) &&
                        Arrays.equals(id1.bsmArgs, id2.bsmArgs);

            case AbstractInsnNode.JUMP_INSN:
                LabelNode label1 = ((JumpInsnNode) n1).label;
                LabelNode label2 = ((JumpInsnNode) n2).label;
                return positions1.get(label1).equals(positions2.get(label2));

            case AbstractInsnNode.LABEL:
                return true;

            case AbstractInsnNode.LDC_INSN:
                return Objects.equals(((LdcInsnNode) n1).cst, ((LdcInsnNode) n2).cst);

            case AbstractInsnNode.IINC_INSN:
                IincInsnNode i1 = (IincInsnNode) n1;
                IincInsnNode i2 = (IincInsnNode) n2;
                return i1.var == i2.var && i1.incr == i2.incr;

            case AbstractInsnNode.TABLESWITCH_INSN:
                TableSwitchInsnNode ts1 = (TableSwitchInsnNode) n1;
                TableSwitchInsnNode ts2 = (TableSwitchInsnNode) n2;
                if (ts1.min != ts2.min || ts1.max != ts2.max) return false;
                if (!positions1.get(ts1.dflt).equals(positions2.get(ts2.dflt))) return false;
                if (ts1.labels.size() != ts2.labels.size()) return false;
                for (int i = 0; i < ts1.labels.size(); i++) {
                    if (!positions1.get(ts1.labels.get(i)).equals(positions2.get(ts2.labels.get(i)))) {
                        return false;
                    }
                }
                return true;

            case AbstractInsnNode.LOOKUPSWITCH_INSN:
                LookupSwitchInsnNode ls1 = (LookupSwitchInsnNode) n1;
                LookupSwitchInsnNode ls2 = (LookupSwitchInsnNode) n2;
                if (!positions1.get(ls1.dflt).equals(positions2.get(ls2.dflt))) return false;
                if (!ls1.keys.equals(ls2.keys)) return false;
                if (ls1.labels.size() != ls2.labels.size()) return false;
                for (int i = 0; i < ls1.labels.size(); i++) {
                    if (!positions1.get(ls1.labels.get(i)).equals(positions2.get(ls2.labels.get(i)))) {
                        return false;
                    }
                }
                return true;

            case AbstractInsnNode.MULTIANEWARRAY_INSN:
                MultiANewArrayInsnNode ma1 = (MultiANewArrayInsnNode) n1;
                MultiANewArrayInsnNode ma2 = (MultiANewArrayInsnNode) n2;
                return Objects.equals(ma1.desc, ma2.desc) && ma1.dims == ma2.dims;

            default:
                throw new IllegalArgumentException("Unknown instruction type: " + n1.getType());
        }
    }

    private static int insnHashCode(AbstractInsnNode insn, Map<LabelNode, Integer> labelPositions) {
        int result = insn.getOpcode();

        switch (insn.getType()) {
            case AbstractInsnNode.INSN:
                break;

            case AbstractInsnNode.INT_INSN:
                result = 31 * result + ((IntInsnNode) insn).operand;
                break;

            case AbstractInsnNode.VAR_INSN:
                result = 31 * result + ((VarInsnNode) insn).var;
                break;

            case AbstractInsnNode.TYPE_INSN:
                result = 31 * result + Objects.hashCode(((TypeInsnNode) insn).desc);
                break;

            case AbstractInsnNode.FIELD_INSN:
                FieldInsnNode f = (FieldInsnNode) insn;
                result = 31 * result + Objects.hashCode(f.owner);
                result = 31 * result + Objects.hashCode(f.name);
                result = 31 * result + Objects.hashCode(f.desc);
                break;

            case AbstractInsnNode.METHOD_INSN:
                MethodInsnNode m = (MethodInsnNode) insn;
                result = 31 * result + Objects.hashCode(m.owner);
                result = 31 * result + Objects.hashCode(m.name);
                result = 31 * result + Objects.hashCode(m.desc);
                break;

            case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
                InvokeDynamicInsnNode id = (InvokeDynamicInsnNode) insn;
                result = 31 * result + Objects.hashCode(id.name);
                result = 31 * result + Objects.hashCode(id.desc);
                result = 31 * result + Objects.hashCode(id.bsm);
                result = 31 * result + Arrays.hashCode(id.bsmArgs);
                break;

            case AbstractInsnNode.JUMP_INSN:
                LabelNode label = ((JumpInsnNode) insn).label;
                result = 31 * result + labelPositions.get(label);
                break;

            case AbstractInsnNode.LABEL:
                break;

            case AbstractInsnNode.LDC_INSN:
                result = 31 * result + Objects.hashCode(((LdcInsnNode) insn).cst);
                break;

            case AbstractInsnNode.IINC_INSN:
                IincInsnNode iinc = (IincInsnNode) insn;
                result = 31 * result + iinc.var;
                result = 31 * result + iinc.incr;
                break;

            case AbstractInsnNode.TABLESWITCH_INSN:
                TableSwitchInsnNode ts = (TableSwitchInsnNode) insn;
                result = 31 * result + ts.min;
                result = 31 * result + ts.max;
                result = 31 * result + labelPositions.get(ts.dflt);
                for (LabelNode l : ts.labels) {
                    result = 31 * result + labelPositions.get(l);
                }
                break;

            case AbstractInsnNode.LOOKUPSWITCH_INSN:
                LookupSwitchInsnNode ls = (LookupSwitchInsnNode) insn;
                result = 31 * result + labelPositions.get(ls.dflt);
                result = 31 * result + ls.keys.hashCode();
                for (LabelNode l : ls.labels) {
                    result = 31 * result + labelPositions.get(l);
                }
                break;

            case AbstractInsnNode.MULTIANEWARRAY_INSN:
                MultiANewArrayInsnNode ma = (MultiANewArrayInsnNode) insn;
                result = 31 * result + Objects.hashCode(ma.desc);
                result = 31 * result + ma.dims;
                break;

            default:
                throw new IllegalArgumentException("Unknown instruction type: " + insn.getType());
        }

        return result;
    }
}
