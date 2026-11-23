package cn.pig.compat.client.event;

import net.minecraft.client.renderer.RenderGlobal;
import cn.pig.compat.fml.common.eventhandler.Event;

public class RenderWorldLastEvent extends Event {
    public final RenderGlobal context;
    public final float partialTicks;

    public RenderWorldLastEvent(RenderGlobal context, float partialTicks) {
        this.context = context;
        this.partialTicks = partialTicks;
    }

                                                             
    public RenderWorldLastEvent(float partialTicks) {
        this(net.minecraft.client.Minecraft.getMinecraft() == null
                ? null : net.minecraft.client.Minecraft.getMinecraft().renderGlobal, partialTicks);
    }
}
