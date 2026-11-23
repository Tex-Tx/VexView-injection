package cn.pig.asm.transform;

import cn.pig.asm.ASMHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

   
                                                              
   
public final class TransformGuiNewChat {

    private static final String HOOKS = "cn/pig/asm/hooks/OverlayHooks";

    private TransformGuiNewChat() {
    }

    public static byte[] transform(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        MethodNode draw = ASMHelper.findMethod(cn, "(I)V", "drawChat", "drawChat");
        if (draw == null) {
            return null;
        }
        ASMHelper.injectHead(draw, HOOKS, "onChat",
                "(Lnet/minecraft/client/gui/GuiNewChat;Lcn/pig/asm/CallbackInfo;)V", true, Type.VOID_TYPE, false);
        return ASMHelper.write(cr, cn);
    }
}
