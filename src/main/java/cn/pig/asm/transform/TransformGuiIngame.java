package cn.pig.asm.transform;

import cn.pig.asm.ASMHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

   
                                                                         
   
public final class TransformGuiIngame {

    private static final String HOOKS = "cn/pig/asm/hooks/OverlayHooks";
    private static final String CI = "Lcn/pig/asm/CallbackInfo;";

    private TransformGuiIngame() {
    }

    public static byte[] transform(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        boolean changed = false;

                                                               
        MethodNode overlay = ASMHelper.findMethod(cn, "(F)V", "renderGameOverlay", "renderGameOverlay");
        if (overlay != null) {
            changed |= ASMHelper.injectReturnSite(overlay, Type.VOID_TYPE, "onCrosshair",
                    new ASMHelper.ReturnSiteEditor() {
                        @Override
                        public void edit(InsnList list, MethodNode method, int callbackSlot, int valueSlot) {
                            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "onCrosshair", "()V", false));
                        }
                    });
        }

        MethodNode boss = ASMHelper.findMethod(cn, "()V", "renderBossHealth", "renderBossHealth");
        if (boss != null) {
            ASMHelper.injectHead(boss, HOOKS, "onBossHealth",
                    "(Lnet/minecraft/client/gui/GuiIngame;" + CI + ")V", true, Type.VOID_TYPE, false);
            changed = true;
        }

        MethodNode stats = ASMHelper.findMethod(cn,
                "(Lnet/minecraft/client/gui/ScaledResolution;)V", "renderPlayerStats", "renderPlayerStats");
        if (stats != null) {
            ASMHelper.injectHead(stats, HOOKS, "onPlayerStats",
                    "(Lnet/minecraft/client/gui/GuiIngame;Lnet/minecraft/client/gui/ScaledResolution;" + CI + ")V",
                    true, Type.VOID_TYPE, false);
            changed = true;
        }

        MethodNode exp = ASMHelper.findMethod(cn,
                "(Lnet/minecraft/client/gui/ScaledResolution;I)V", "renderExpBar", "renderExpBar");
        if (exp != null) {
            ASMHelper.injectHead(exp, HOOKS, "onExpBar",
                    "(Lnet/minecraft/client/gui/GuiIngame;Lnet/minecraft/client/gui/ScaledResolution;I" + CI + ")V",
                    true, Type.VOID_TYPE, false);
            changed = true;
        }

        MethodNode tip = ASMHelper.findMethod(cn,
                "(Lnet/minecraft/client/gui/ScaledResolution;F)V", "renderTooltip", "renderTooltip");
        if (tip != null) {
            ASMHelper.injectHead(tip, HOOKS, "onHotbar",
                    "(Lnet/minecraft/client/gui/GuiIngame;Lnet/minecraft/client/gui/ScaledResolution;F" + CI + ")V",
                    true, Type.VOID_TYPE, false);
            changed = true;
        }

        return changed ? ASMHelper.write(cr, cn) : null;
    }
}
