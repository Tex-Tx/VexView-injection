package cn.pig.asm.transform;

import cn.pig.asm.ASMHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

   
                                                                     
   
public final class TransformGuiScreen {

    private static final String HOOKS = "cn/pig/asm/hooks/GuiHooks";

    private TransformGuiScreen() {
    }

    public static byte[] transform(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        MethodNode draw = ASMHelper.findMethod(cn, "(IIF)V", "drawScreen", "drawScreen");
        if (draw == null) {
            return null;
        }
        ASMHelper.injectHead(draw, HOOKS, "onDrawScreenPre",
                "(Lnet/minecraft/client/gui/GuiScreen;IIFLcn/pig/asm/CallbackInfo;)V",
                true, Type.VOID_TYPE, false);
        ASMHelper.injectReturnSite(draw, Type.VOID_TYPE, "onDrawScreenPost", new ASMHelper.ReturnSiteEditor() {
            @Override
            public void edit(InsnList list, MethodNode method, int callbackSlot, int valueSlot) {
                list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                list.add(new VarInsnNode(Opcodes.ILOAD, 1));
                list.add(new VarInsnNode(Opcodes.ILOAD, 2));
                list.add(new VarInsnNode(Opcodes.FLOAD, 3));
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "onDrawScreenPost",
                        "(Lnet/minecraft/client/gui/GuiScreen;IIF)V", false));
            }
        });
        return ASMHelper.write(cr, cn);
    }
}
