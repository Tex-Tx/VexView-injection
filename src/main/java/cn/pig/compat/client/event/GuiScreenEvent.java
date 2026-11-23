package cn.pig.compat.client.event;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import cn.pig.compat.fml.common.eventhandler.Cancelable;
import cn.pig.compat.fml.common.eventhandler.Event;

   
                                                                  
   
public class GuiScreenEvent extends Event {

                   
    public final GuiScreen gui;

    public GuiScreenEvent(GuiScreen gui) {
        this.gui = gui;
    }

    public static class InitGuiEvent extends GuiScreenEvent {
        public List<GuiButton> buttonList;

        public InitGuiEvent(GuiScreen gui, List<GuiButton> buttonList) {
            super(gui);
            this.buttonList = buttonList;
        }

        @Cancelable
        public static class Pre extends InitGuiEvent {
            public Pre(GuiScreen gui, List<GuiButton> buttonList) {
                super(gui, buttonList);
            }
        }

        public static class Post extends InitGuiEvent {
            public Post(GuiScreen gui, List<GuiButton> buttonList) {
                super(gui, buttonList);
            }
        }
    }

    public static class DrawScreenEvent extends GuiScreenEvent {
        public final int mouseX;
        public final int mouseY;
        public final float renderPartialTicks;

        public DrawScreenEvent(GuiScreen gui, int mouseX, int mouseY, float renderPartialTicks) {
            super(gui);
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.renderPartialTicks = renderPartialTicks;
        }

        @Cancelable
        public static class Pre extends DrawScreenEvent {
            public Pre(GuiScreen gui, int mouseX, int mouseY, float renderPartialTicks) {
                super(gui, mouseX, mouseY, renderPartialTicks);
            }
        }

        public static class Post extends DrawScreenEvent {
            public Post(GuiScreen gui, int mouseX, int mouseY, float renderPartialTicks) {
                super(gui, mouseX, mouseY, renderPartialTicks);
            }
        }
    }

    public static class ActionPerformedEvent extends GuiScreenEvent {
        public GuiButton button;
        public List<GuiButton> buttonList;

        public ActionPerformedEvent(GuiScreen gui, GuiButton button, List<GuiButton> buttonList) {
            super(gui);
            this.button = button;
            this.buttonList = new ArrayList<GuiButton>(buttonList);
        }

        @Cancelable
        public static class Pre extends ActionPerformedEvent {
            public Pre(GuiScreen gui, GuiButton button, List<GuiButton> buttonList) {
                super(gui, button, buttonList);
            }
        }

        public static class Post extends ActionPerformedEvent {
            public Post(GuiScreen gui, GuiButton button, List<GuiButton> buttonList) {
                super(gui, button, buttonList);
            }
        }
    }

    public static class MouseInputEvent extends GuiScreenEvent {
        public MouseInputEvent(GuiScreen gui) {
            super(gui);
        }

        @Cancelable
        public static class Pre extends MouseInputEvent {
            public Pre(GuiScreen gui) {
                super(gui);
            }
        }

        public static class Post extends MouseInputEvent {
            public Post(GuiScreen gui) {
                super(gui);
            }
        }
    }

    public static class KeyboardInputEvent extends GuiScreenEvent {
        public KeyboardInputEvent(GuiScreen gui) {
            super(gui);
        }

        @Cancelable
        public static class Pre extends KeyboardInputEvent {
            public Pre(GuiScreen gui) {
                super(gui);
            }
        }

        public static class Post extends KeyboardInputEvent {
            public Post(GuiScreen gui) {
                super(gui);
            }
        }
    }
}
