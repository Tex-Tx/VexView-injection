package cn.pig.asm.transform;

import cn.pig.asm.ASMHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

   
                                                     
   
public final class TransformGuiContainer {

    private static final String HOOKS = "cn/pig/asm/hooks/GuiHooks";
    private static final String GUI = "Lnet/minecraft/client/gui/inventory/GuiContainer;";

    private TransformGuiContainer() {
    }

    public static byte[] transform(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        MethodNode draw = ASMHelper.findMethod(cn, "(IIF)V", "drawScreen", "drawScreen");
        if (draw == null) {
            return null;
        }

        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        int exSlot = draw.maxLocals;
        draw.maxLocals += 1;

        draw.instructions.insert(start);
        InsnList head = new InsnList();
        head.add(new VarInsnNode(Opcodes.ALOAD, 0));
        head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "onContainerDraw", "(" + GUI + ")V", false));
        draw.instructions.insert(start, head);

        InsnList tail = new InsnList();
        tail.add(end);
        tail.add(handler);
        tail.add(new VarInsnNode(Opcodes.ASTORE, exSlot));
        tail.add(new VarInsnNode(Opcodes.ALOAD, 0));
        tail.add(new VarInsnNode(Opcodes.ALOAD, exSlot));
        tail.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "onContainerDrawThrown",
                "(" + GUI + "Ljava/lang/Throwable;)V", false));
        tail.add(new InsnNode(Opcodes.RETURN));
        draw.instructions.add(tail);

        if (draw.tryCatchBlocks == null) {
            draw.tryCatchBlocks = new java.util.ArrayList<TryCatchBlockNode>();
        }
        draw.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, "java/lang/RuntimeException"));

        return ASMHelper.write(cr, cn);
    }
}
