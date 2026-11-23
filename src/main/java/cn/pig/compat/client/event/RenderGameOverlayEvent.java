package cn.pig.compat.client.event;

import net.minecraft.client.gui.ScaledResolution;
import cn.pig.compat.fml.common.eventhandler.Cancelable;
import cn.pig.compat.fml.common.eventhandler.Event;

   
                                                                              
                                                        
   
@Cancelable
public class RenderGameOverlayEvent extends Event {

    public enum ElementType {
        ALL,
        HELMET,
        PORTAL,
        CROSSHAIRS,
        BOSSHEALTH,
        ARMOR,
        HEALTH,
        FOOD,
        AIR,
        HOTBAR,
        EXPERIENCE,
        TEXT,
        HEALTHMOUNT,
        JUMPBAR,
        CHAT,
        PLAYER_LIST,
        DEBUG
    }

    public final float partialTicks;
    public final ScaledResolution resolution;
    public final ElementType type;

    public RenderGameOverlayEvent(float partialTicks, ScaledResolution resolution) {
        this.partialTicks = partialTicks;
        this.resolution = resolution;
        this.type = null;
    }

    private RenderGameOverlayEvent(RenderGameOverlayEvent parent, ElementType type) {
        this.partialTicks = parent.partialTicks;
        this.resolution = parent.resolution;
        this.type = type;
    }

    public static class Pre extends RenderGameOverlayEvent {
        public Pre(RenderGameOverlayEvent parent, ElementType type) {
            super(parent, type);
        }

                       
        public Pre(float partialTicks, ScaledResolution resolution, ElementType type) {
            this(new RenderGameOverlayEvent(partialTicks, resolution), type);
        }
    }

    public static class Post extends RenderGameOverlayEvent {
        public Post(RenderGameOverlayEvent parent, ElementType type) {
            super(parent, type);
        }

        @Override
        public boolean isCancelable() {
            return false;
        }
    }
}
