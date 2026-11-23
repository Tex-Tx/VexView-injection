package cn.pig.asm;

import cn.pig.asm.ns.MappingKind;
import cn.pig.asm.ns.NamespaceRegistry;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

   
                                                                  
  
                                                               
                                                                            
                                                          
                                                              
   
public class SafeClassWriter extends ClassWriter {

                                                         
    public static final ThreadLocal<MappingKind> RUNTIME_KIND = new ThreadLocal<MappingKind>();

    public SafeClassWriter(ClassReader reader, int flags) {
        super(reader, flags);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        if (type1.equals(type2)) {
            return type1;
        }
        MappingKind kind = RUNTIME_KIND.get();
        Class<?> c1 = load(resolveToRuntime(type1, kind));
        Class<?> c2 = load(resolveToRuntime(type2, kind));
        if (c1 == null || c2 == null) {
            return "java/lang/Object";
        }
        try {
            if (c1.isAssignableFrom(c2)) {
                return type1;
            }
            if (c2.isAssignableFrom(c1)) {
                return type2;
            }
            if (!c1.isInterface() && !c2.isInterface()) {
                do {
                    c1 = c1.getSuperclass();
                } while (c1 != null && !c1.isAssignableFrom(c2));
                if (c1 == null) {
                    return "java/lang/Object";
                }
                                                       
                return resolveToMcp(c1.getName().replace('.', '/'), kind);
            }
        } catch (Throwable ignore) {
        }
        return "java/lang/Object";
    }

                                                                     
    private static String resolveToRuntime(String internal, MappingKind kind) {
        if (kind == null || kind == MappingKind.MCP || internal == null
                || !internal.startsWith("net/minecraft/")) {
            return internal;
        }
        return NamespaceRegistry.fromMcp(kind).mapClassName(internal);
    }

                                        
    private static String resolveToMcp(String internal, MappingKind kind) {
        if (kind == null || kind == MappingKind.MCP || internal == null
                || !internal.startsWith("net/minecraft/")) {
            return internal;
        }
        return NamespaceRegistry.toMcp(kind).mapClassName(internal);
    }

    private static Class<?> load(String internal) {
        if (internal == null) {
            return null;
        }
        String dotted = internal.replace('/', '.');
        ClassLoader[] loaders = new ClassLoader[] {
                Thread.currentThread().getContextClassLoader(),
                SafeClassWriter.class.getClassLoader(),
                ClassLoader.getSystemClassLoader(),
        };
        for (ClassLoader cl : loaders) {
                                                                    
                                                                            
            Class<?> c = findLoaded(cl, dotted);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    private static Class<?> findLoaded(ClassLoader cl, String dottedName) {
        if (cl == null) {
            return null;
        }
        try {
            java.lang.reflect.Method m =
                    ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            m.setAccessible(true);
            return (Class<?>) m.invoke(cl, dottedName);
        } catch (Throwable t) {
            return null;
        }
    }
}
