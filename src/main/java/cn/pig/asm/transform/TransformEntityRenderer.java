package cn.pig.asm.transform;

import cn.pig.asm.ASMHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

   
                                                              
   
public final class TransformEntityRenderer {

    private static final String HOOKS = "cn/pig/asm/hooks/WorldHooks";

    private TransformEntityRenderer() {
    }

    public static byte[] transform(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        MethodNode renderWorld = ASMHelper.findMethod(cn, "(FJ)V", "renderWorld", "renderWorld");
        if (renderWorld == null) {
            return null;
        }
        boolean changed = ASMHelper.injectReturnSite(renderWorld, Type.VOID_TYPE, "onRenderWorldLast",
                new ASMHelper.ReturnSiteEditor() {
                    @Override
                    public void edit(InsnList list, MethodNode method, int callbackSlot, int valueSlot) {
                        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS,
                                "onRenderWorldLast", "()V", false));
                    }
                });
        return changed ? ASMHelper.write(cr, cn) : null;
    }
}
