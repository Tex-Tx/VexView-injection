package cn.pig.asm.transform;

import cn.pig.asm.ASMHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

   
             
       
                                                                     
                                                                                                
        
   
public final class TransformMinecraft {

    private static final String GUI_HOOKS = "cn/pig/asm/hooks/GuiHooks";
    private static final String INPUT_HOOKS = "cn/pig/asm/hooks/InputHooks";

    private TransformMinecraft() {
    }

    public static byte[] transform(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        boolean changed = false;

        MethodNode display = ASMHelper.findMethod(cn,
                "(Lnet/minecraft/client/gui/GuiScreen;)V", "displayGuiScreen", "displayGuiScreen");
        if (display != null) {
            injectGuiOpen(display);
            changed = true;
        }

        MethodNode tick = ASMHelper.findMethod(cn, "()V", "runTick", "runTick");
        if (tick != null && injectKeyLoop(tick)) {
            changed = true;
        }

        return changed ? ASMHelper.write(cr, cn) : null;
    }

       
            
                         
                                                  
                                              
        
             
       
    private static void injectGuiOpen(MethodNode mn) {
        InsnList list = new InsnList();
        LabelNode cont = new LabelNode();
        list.add(new VarInsnNode(Opcodes.ALOAD, 1));
        list.add(new JumpInsnNode(Opcodes.IFNULL, cont));
        list.add(new VarInsnNode(Opcodes.ALOAD, 1));
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, GUI_HOOKS, "onDisplayGuiScreen",
                "(Lnet/minecraft/client/gui/GuiScreen;)Lnet/minecraft/client/gui/GuiScreen;", false));
        list.add(new VarInsnNode(Opcodes.ASTORE, 1));
        list.add(new VarInsnNode(Opcodes.ALOAD, 1));
        list.add(new JumpInsnNode(Opcodes.IFNONNULL, cont));
        list.add(new InsnNode(Opcodes.RETURN));
        list.add(cont);
        mn.instructions.insert(list);
    }

       
                                                                     
       
    private static boolean injectKeyLoop(MethodNode mn) {
        MethodInsnNode next = null;
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode mi = (MethodInsnNode) insn;
                if ("org/lwjgl/input/Keyboard".equals(mi.owner)
                        && "next".equals(mi.name)
                        && "()Z".equals(mi.desc)) {
                    next = mi;
                    break;
                }
            }
        }
        if (next == null) {
            return false;
        }
                                                  
        AbstractInsnNode prev = next.getPrevious();
        while (prev != null && !(prev instanceof LabelNode)) {
            prev = prev.getPrevious();
        }
        if (prev == null) {
            return false;
        }
        LabelNode loopStart = (LabelNode) prev;
                                 
        for (AbstractInsnNode insn = next; insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.GOTO && insn instanceof JumpInsnNode
                    && ((JumpInsnNode) insn).label == loopStart) {
                InsnList list = new InsnList();
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, INPUT_HOOKS, "fireKeyInput", "()V", false));
                mn.instructions.insertBefore(insn, list);
                return true;
            }
        }
        return false;
    }
}
