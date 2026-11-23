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

   
                        
       
                                                                
                                                              
                                                                     
        
   
public final class TransformNetHandlerPlayClient {

    private static final String HOOKS = "cn/pig/asm/hooks/NetHooks";

    private TransformNetHandlerPlayClient() {
    }

    public static byte[] transform(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        boolean changed = false;

        MethodNode payload = ASMHelper.findMethod(cn,
                "(Lnet/minecraft/network/play/server/S3FPacketCustomPayload;)V",
                "handleCustomPayload", "handleCustomPayload");
        if (payload != null) {
            injectCustomPayload(payload);
            changed = true;
        }

        MethodNode join = ASMHelper.findMethod(cn,
                "(Lnet/minecraft/network/play/server/S01PacketJoinGame;)V", "handleJoinGame", "handleJoinGame");
        if (join != null) {
            InsnList list = new InsnList();
            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "onJoinGame", "()V", false));
            join.instructions.insert(list);
            changed = true;
        }

        return changed ? ASMHelper.write(cr, cn) : null;
    }

    private static void injectCustomPayload(MethodNode mn) {
        InsnList block = new InsnList();
        LabelNode cont = new LabelNode();
        block.add(new VarInsnNode(Opcodes.ALOAD, 0));
        block.add(new VarInsnNode(Opcodes.ALOAD, 1));
        block.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "onCustomPayload",
                "(Ljava/lang/Object;Lnet/minecraft/network/play/server/S3FPacketCustomPayload;)Z", false));
        block.add(new JumpInsnNode(Opcodes.IFEQ, cont));
        block.add(new InsnNode(Opcodes.RETURN));
        block.add(cont);

                                                                 
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode mi = (MethodInsnNode) insn;
                if ("net/minecraft/network/PacketThreadUtil".equals(mi.owner)
                        && "checkThreadAndEnqueue".equals(mi.name)) {
                    mn.instructions.insert(mi, block);
                    return;
                }
            }
        }
        mn.instructions.insert(block);
    }
}
