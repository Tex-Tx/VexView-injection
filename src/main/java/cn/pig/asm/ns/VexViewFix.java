package cn.pig.asm.ns;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

   
                                                    
  
                                                              
                                                                      
   
public final class VexViewFix {

    private VexViewFix() {
    }

    public static byte[] apply(byte[] classBytes) {
        try {
            ClassReader cr = new ClassReader(classBytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            boolean changed = false;
            for (Object mo : cn.methods) {
                MethodNode m = (MethodNode) mo;
                for (Object io : m.instructions.toArray()) {
                    AbstractInsnNode insn = (AbstractInsnNode) io;
                    if (insn instanceof MethodInsnNode) {
                        MethodInsnNode mi = (MethodInsnNode) insn;
                        if ("getSlotUnderMouse".equals(mi.name)
                                && "()Lnet/minecraft/inventory/Slot;".equals(mi.desc)) {
                            m.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC,
                                    "cn/pig/runtime/VexGuiStateGuard", "slotUnderMouse",
                                    "(Ljava/lang/Object;)Lnet/minecraft/inventory/Slot;", false));
                            changed = true;
                        }
                    }
                }
            }
            if (!changed) {
                return classBytes;
            }
                                   
            ClassWriter cw = new ClassWriter(0);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            return classBytes;
        }
    }
}
