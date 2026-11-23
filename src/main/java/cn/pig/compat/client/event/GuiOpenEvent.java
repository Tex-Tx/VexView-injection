package cn.pig.compat.client.event;

import net.minecraft.client.gui.GuiScreen;
import cn.pig.compat.fml.common.eventhandler.Cancelable;
import cn.pig.compat.fml.common.eventhandler.Event;

@Cancelable
public class GuiOpenEvent extends Event {
    public GuiScreen gui;

    public GuiOpenEvent(GuiScreen gui) {
        this.gui = gui;
    }
}
