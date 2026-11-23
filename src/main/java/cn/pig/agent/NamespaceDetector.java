package cn.pig.agent;

import cn.pig.asm.ns.MappingKind;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

   
                                         
  
                    
       
                                                    
                                                                                   
                                                                          
                              
        
   
public final class NamespaceDetector {

    private static final String MCP_MINECRAFT = "net.minecraft.client.Minecraft";
    private static final String NOTCH_MINECRAFT = "ave";

    private NamespaceDetector() {
    }

    public static MappingKind detect(Instrumentation inst) {
        Class<?> mcpMinecraft = findLoaded(inst, MCP_MINECRAFT);
        if (mcpMinecraft != null) {
            return hasField(mcpMinecraft, "thePlayer") ? MappingKind.MCP : MappingKind.SRG;
        }
        if (findLoaded(inst, NOTCH_MINECRAFT) != null) {
            return MappingKind.NOTCH;
        }
        return MappingKind.NOTCH;
    }

       
                                                     
                                                                      
                                                    
       
    public static MappingKind detect(ClassLoader loader) {
        for (ClassLoader l = loader; l != null; l = l.getParent()) {
            Class<?> mcpMinecraft = findLoaded(l, MCP_MINECRAFT);
            if (mcpMinecraft != null) {
                return hasField(mcpMinecraft, "thePlayer") ? MappingKind.MCP : MappingKind.SRG;
            }
            if (findLoaded(l, NOTCH_MINECRAFT) != null) {
                return MappingKind.NOTCH;
            }
        }
        return MappingKind.NOTCH;
    }

       
                                                             
                                                   
                                            
       
    public static MappingKind detectFromBytes(byte[] classBytes) {
        try {
            org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(classBytes).accept(node, 0);
            for (Object o : node.fields) {
                String name = ((org.objectweb.asm.tree.FieldNode) o).name;
                if (name.startsWith("field_")) {
                    return MappingKind.SRG;
                }
            }
            for (Object o : node.methods) {
                String name = ((org.objectweb.asm.tree.MethodNode) o).name;
                if (name.startsWith("func_")) {
                    return MappingKind.SRG;
                }
            }
        } catch (Throwable ignored) {
        }
        return MappingKind.MCP;
    }

    private static Class<?> findLoaded(ClassLoader loader, String name) {
        if (loader == null) {
            return null;
        }
        try {
            Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            m.setAccessible(true);
            return (Class<?>) m.invoke(loader, name);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Class<?> findLoaded(Instrumentation inst, String name) {
        if (inst == null) {
            return null;
        }
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (c != null && name.equals(c.getName())) {
                return c;
            }
        }
        return null;
    }

    private static boolean hasField(Class<?> type, String name) {
        Class<?> c = type;
        while (c != null) {
            try {
                c.getDeclaredField(name);
                return true;
            } catch (Throwable ignored) {
            }
            c = c.getSuperclass();
        }
        return false;
    }
}
