package cn.pig.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class ASMHelper implements Opcodes {
    public static final String CALLBACK_INFO = "cn/pig/asm/CallbackInfo";
    public static final String CALLBACK_INFO_RETURNABLE = "cn/pig/asm/CallbackInfoReturnable";

    private ASMHelper() {
    }

    @FunctionalInterface
    public interface InsnEditor {
        void edit(InsnList list);
    }

    @FunctionalInterface
    public interface CallEditor {
        void edit(InsnList list, MethodInsnNode call);
    }

    @FunctionalInterface
    public interface ReturnSiteEditor {
        void edit(InsnList list, MethodNode method, int callbackSlot, int valueSlot);
    }

    public static MethodNode findMethod(ClassNode cn, String desc, String srg, String mcp) {
        for (Object o : cn.methods) {
            MethodNode mn = (MethodNode) o;
            if (mn.desc.equals(desc) && (mn.name.equals(srg) || mn.name.equals(mcp))) {
                return mn;
            }
        }
        return null;
    }

    public static void injectHead(MethodNode mn, String hookOwner, String hookName, String hookDesc,
                                  boolean cancellable, Type retType, boolean returnable) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(ALOAD, 0));
        int slot = 1;
        for (Type arg : Type.getArgumentTypes(mn.desc)) {
            list.add(new VarInsnNode(arg.getOpcode(ILOAD), slot));
            slot += arg.getSize();
        }
        int ciSlot = slot;
        String ciType = returnable ? CALLBACK_INFO_RETURNABLE : CALLBACK_INFO;
        list.add(new TypeInsnNode(NEW, ciType));
        list.add(new InsnNode(DUP));
        list.add(new LdcInsnNode(hookName));
        if (returnable) {
            list.add(new InsnNode(ACONST_NULL));
            list.add(new MethodInsnNode(INVOKESPECIAL, ciType, "<init>", "(Ljava/lang/String;Ljava/lang/Object;)V", false));
        } else {
            list.add(new MethodInsnNode(INVOKESPECIAL, ciType, "<init>", "(Ljava/lang/String;)V", false));
        }
        list.add(new VarInsnNode(ASTORE, ciSlot));
        list.add(new VarInsnNode(ALOAD, ciSlot));
        list.add(new MethodInsnNode(INVOKESTATIC, hookOwner, hookName, hookDesc, false));
        if (cancellable) {
            LabelNode cont = new LabelNode();
            list.add(new VarInsnNode(ALOAD, ciSlot));
            list.add(new MethodInsnNode(INVOKEVIRTUAL, ciType, "isCancelled", "()Z", false));
            list.add(new JumpInsnNode(IFEQ, cont));
            if (retType != Type.VOID_TYPE) {
                loadReturnValue(list, ciSlot, ciType, retType);
            }
            list.add(new InsnNode(retType.getOpcode(IRETURN)));
            list.add(cont);
        }
        mn.instructions.insert(list);
    }

    public static void injectHeadRaw(MethodNode mn, InsnEditor editor) {
        InsnList list = new InsnList();
        editor.edit(list);
        mn.instructions.insert(list);
    }

    public static boolean injectReturnSite(MethodNode mn, String hookOwner, String hookName, String hookDesc, Type retType) {
        return injectReturnSite(mn, retType, hookName, (list, method, callbackSlot, valueSlot) -> {
            loadThis(list);
            loadCallbackInfo(list, callbackSlot);
            invokeStatic(list, hookOwner, hookName, hookDesc);
        });
    }

    public static boolean injectReturnSite(MethodNode mn, Type retType, String callbackName, ReturnSiteEditor editor) {
        boolean changed = false;
        int returnOpcode = retType.getOpcode(IRETURN);
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != returnOpcode) {
                continue;
            }
            InsnList list = new InsnList();
            if (retType.getSort() == Type.VOID) {
                int ciSlot = newCallbackInfo(mn, list, callbackName);
                editor.edit(list, mn, ciSlot, -1);
            } else {
                int valueSlot = allocLocal(mn, retType);
                list.add(new VarInsnNode(retType.getOpcode(ISTORE), valueSlot));
                int ciSlot = newCallbackInfoReturnable(mn, list, callbackName, retType, valueSlot);
                editor.edit(list, mn, ciSlot, valueSlot);
                cancelCheck(list, ciSlot, retType, true);
                list.add(new VarInsnNode(retType.getOpcode(ILOAD), valueSlot));
            }
            mn.instructions.insertBefore(insn, list);
            changed = true;
        }
        return changed;
    }

    public static boolean injectBeforeCall(MethodNode mn, String owner, String desc, InsnEditor editor, String... names) {
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode && matchesCall((MethodInsnNode) insn, owner, desc, names)) {
                InsnList list = new InsnList();
                editor.edit(list);
                mn.instructions.insertBefore(insn, list);
                return true;
            }
        }
        return false;
    }

    public static boolean injectAfterCall(MethodNode mn, String owner, String desc, InsnEditor editor, String... names) {
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode && matchesCall((MethodInsnNode) insn, owner, desc, names)) {
                InsnList list = new InsnList();
                editor.edit(list);
                mn.instructions.insert(insn, list);
                return true;
            }
        }
        return false;
    }

    public static boolean replaceCall(MethodNode mn, String owner, String desc, CallEditor editor, String... names) {
        boolean changed = false;
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode && matchesCall((MethodInsnNode) insn, owner, desc, names)) {
                MethodInsnNode call = (MethodInsnNode) insn;
                InsnList list = new InsnList();
                editor.edit(list, call);
                mn.instructions.insertBefore(insn, list);
                mn.instructions.remove(insn);
                changed = true;
            }
        }
        return changed;
    }

    public static boolean matchesCall(MethodInsnNode call, String owner, String desc, String... names) {
        if (!call.owner.equals(owner) || !call.desc.equals(desc)) {
            return false;
        }
        for (String n : names) {
            if (call.name.equals(n)) {
                return true;
            }
        }
        return false;
    }

    public static int allocLocal(MethodNode mn, Type type) {
        int slot = mn.maxLocals;
        mn.maxLocals += type.getSize();
        return slot;
    }

    public static int newCallbackInfo(MethodNode mn, InsnList list, String name) {
        int slot = allocLocal(mn, Type.getObjectType(CALLBACK_INFO));
        list.add(new TypeInsnNode(NEW, CALLBACK_INFO));
        list.add(new InsnNode(DUP));
        list.add(new LdcInsnNode(name));
        list.add(new MethodInsnNode(INVOKESPECIAL, CALLBACK_INFO, "<init>", "(Ljava/lang/String;)V", false));
        list.add(new VarInsnNode(ASTORE, slot));
        return slot;
    }

    public static int newCallbackInfoReturnable(MethodNode mn, InsnList list, String name) {
        int slot = allocLocal(mn, Type.getObjectType(CALLBACK_INFO_RETURNABLE));
        list.add(new TypeInsnNode(NEW, CALLBACK_INFO_RETURNABLE));
        list.add(new InsnNode(DUP));
        list.add(new LdcInsnNode(name));
        list.add(new InsnNode(ACONST_NULL));
        list.add(new MethodInsnNode(INVOKESPECIAL, CALLBACK_INFO_RETURNABLE, "<init>", "(Ljava/lang/String;Ljava/lang/Object;)V", false));
        list.add(new VarInsnNode(ASTORE, slot));
        return slot;
    }

    public static int newCallbackInfoReturnable(MethodNode mn, InsnList list, String name, Type valueType, int valueSlot) {
        int slot = allocLocal(mn, Type.getObjectType(CALLBACK_INFO_RETURNABLE));
        list.add(new TypeInsnNode(NEW, CALLBACK_INFO_RETURNABLE));
        list.add(new InsnNode(DUP));
        list.add(new LdcInsnNode(name));
        if (valueType.getSort() == Type.OBJECT || valueType.getSort() == Type.ARRAY) {
            list.add(new VarInsnNode(ALOAD, valueSlot));
        } else {
            list.add(new VarInsnNode(valueType.getOpcode(ILOAD), valueSlot));
            String box = boxName(valueType);
            list.add(new MethodInsnNode(INVOKESTATIC, box, "valueOf", "(" + valueType.getDescriptor() + ")L" + box + ";", false));
        }
        list.add(new MethodInsnNode(INVOKESPECIAL, CALLBACK_INFO_RETURNABLE, "<init>", "(Ljava/lang/String;Ljava/lang/Object;)V", false));
        list.add(new VarInsnNode(ASTORE, slot));
        return slot;
    }

    public static void loadThis(InsnList list) {
        list.add(new VarInsnNode(ALOAD, 0));
    }

    public static void loadCallbackInfo(InsnList list, int slot) {
        list.add(new VarInsnNode(ALOAD, slot));
    }

    public static void loadLocal(InsnList list, Type type, int slot) {
        list.add(new VarInsnNode(type.getOpcode(ILOAD), slot));
    }

    public static void loadArg(InsnList list, MethodNode mn, int index) {
        Type[] args = Type.getArgumentTypes(mn.desc);
        int slot = (mn.access & ACC_STATIC) == 0 ? 1 : 0;
        for (int i = 0; i < index; i++) {
            slot += args[i].getSize();
        }
        list.add(new VarInsnNode(args[index].getOpcode(ILOAD), slot));
    }

    public static void getField(InsnList list, String owner, String name, String desc) {
        list.add(new FieldInsnNode(GETFIELD, owner, name, desc));
    }

    public static void invokeStatic(InsnList list, String owner, String name, String desc) {
        list.add(new MethodInsnNode(INVOKESTATIC, owner, name, desc, false));
    }

    public static void invokeVirtual(InsnList list, String owner, String name, String desc) {
        list.add(new MethodInsnNode(INVOKEVIRTUAL, owner, name, desc, false));
    }

    public static void cancelCheck(InsnList list, int ciSlot, Type retType, boolean returnable) {
        String ciType = returnable ? CALLBACK_INFO_RETURNABLE : CALLBACK_INFO;
        LabelNode cont = new LabelNode();
        list.add(new VarInsnNode(ALOAD, ciSlot));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, ciType, "isCancelled", "()Z", false));
        list.add(new JumpInsnNode(IFEQ, cont));
        if (retType != Type.VOID_TYPE) {
            loadReturnValue(list, ciSlot, ciType, retType);
        }
        list.add(new InsnNode(retType.getOpcode(IRETURN)));
        list.add(cont);
    }

    private static void loadReturnValue(InsnList list, int ciSlot, String ciType, Type retType) {
        list.add(new VarInsnNode(ALOAD, ciSlot));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, ciType, "getReturnValue", "()Ljava/lang/Object;", false));
        if (retType.getSort() == Type.OBJECT || retType.getSort() == Type.ARRAY) {
            list.add(new TypeInsnNode(CHECKCAST, retType.getInternalName()));
        } else {
            String box = boxName(retType);
            list.add(new TypeInsnNode(CHECKCAST, box));
            list.add(new MethodInsnNode(INVOKEVIRTUAL, box, unboxName(retType), "()" + retType.getDescriptor(), false));
        }
    }

    public static byte[] write(ClassReader cr, ClassNode cn) {
                                                                   
                                                          
                              
                                                                    
        try {
            SafeClassWriter writer = new SafeClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
            cn.accept(writer);
            return writer.toByteArray();
        } catch (Throwable t) {
            SafeClassWriter writer = new SafeClassWriter(cr, ClassWriter.COMPUTE_MAXS);
            cn.accept(writer);
            return writer.toByteArray();
        }
    }

    private static String boxName(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN: return "java/lang/Boolean";
            case Type.BYTE: return "java/lang/Byte";
            case Type.SHORT: return "java/lang/Short";
            case Type.CHAR: return "java/lang/Character";
            case Type.INT: return "java/lang/Integer";
            case Type.FLOAT: return "java/lang/Float";
            case Type.LONG: return "java/lang/Long";
            case Type.DOUBLE: return "java/lang/Double";
            default: return t.getInternalName();
        }
    }

    private static String unboxName(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN: return "booleanValue";
            case Type.BYTE: return "byteValue";
            case Type.SHORT: return "shortValue";
            case Type.CHAR: return "charValue";
            case Type.INT: return "intValue";
            case Type.FLOAT: return "floatValue";
            case Type.LONG: return "longValue";
            case Type.DOUBLE: return "doubleValue";
            default: throw new IllegalArgumentException("no unbox for " + t);
        }
    }
}
