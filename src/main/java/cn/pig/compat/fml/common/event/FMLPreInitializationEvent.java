package cn.pig.compat.fml.common.event;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

   
                                      
   
public class FMLPreInitializationEvent extends FMLStateEvent {

    private final File suggestedConfigFile;

    public FMLPreInitializationEvent() {
        this(new File(new File("config"), "vexview.cfg"));
    }

    public FMLPreInitializationEvent(File suggestedConfigFile) {
        this.suggestedConfigFile = suggestedConfigFile;
    }

    public File getSuggestedConfigurationFile() {
        return suggestedConfigFile;
    }

    public File getModConfigurationDirectory() {
        return suggestedConfigFile == null ? null : suggestedConfigFile.getParentFile();
    }

    public File getSourceFile() {
        return null;
    }

    public Logger getModLog() {
        return LogManager.getLogger("VexView");
    }
}
