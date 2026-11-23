package cn.pig.agent;

import cn.pig.asm.ns.MappingKind;
import cn.pig.asm.ns.NamespaceBridge;
import cn.pig.asm.ns.NamespaceRegistry;

import java.io.File;

   
                                                      
  
                                                  
                                                                                
                                                          
                                           
                                                                    
  
                     
       
                                                                           
                                             
                                                                           
                                                                        
                                               
        
   
public final class NativeBridge {

    private static final String AGENT_PREFIX = "cn/pig/agent/";

    private static volatile NamespaceBridge bridge;
    private static volatile String selfJarPath;
                                  
    private static volatile boolean kindCorrected;
                                                      
    private static volatile ClassLoader gameLoader;
    private static volatile boolean mcMissLogged;

    private NativeBridge() {
    }

                                               
    public static NamespaceBridge bridge() {
        return bridge;
    }

                                                           
    public static String selfJarPath() {
        return selfJarPath;
    }

                                              
    public static void setGameLoader(ClassLoader loader) {
        gameLoader = loader;
        PigLog.log("[Pig] 收到游戏类加载器: " + loader);
    }

       
                                                                   
      
                                           
                                                                                      
       
    public static synchronized void install(String jarPath, String options) {
        PigLog.useJar(jarPath);
        if (bridge != null) {
            PigLog.log("[Pig] NativeBridge 已安装，忽略重复调用");
            return;
        }
        ClassLoader loader = gameLoader != null ? gameLoader : NativeBridge.class.getClassLoader();
        MappingKind kind = parseKind(options);
        if (kind == null) {
            kind = NamespaceDetector.detect(loader);
            if (kind == MappingKind.NOTCH) {
                                                 
                MappingKind ctxKind = NamespaceDetector.detect(Thread.currentThread().getContextClassLoader());
                if (ctxKind != MappingKind.NOTCH) {
                    kind = ctxKind;
                }
            }
            PigLog.log("[Pig] 自动探测命名空间 = " + kind);
        }
        preloadInfra(kind);
        bridge = new NamespaceBridge(kind);
        selfJarPath = jarPath;
        PigLog.log("[Pig] NativeBridge 安装完成，namespace=" + kind
                + "，jar=" + jarPath + "，loader=" + loader);
    }

       
                                                                 
                                          
      
                                       
                                
                                                 
       
    public static byte[] transform(String className, byte[] classBytes) {
        NamespaceBridge b = bridge;
        if (b == null || className == null || classBytes == null) {
            return null;
        }
        String internal = className.replace('.', '/');
        b = correctKind(internal, classBytes);
        try {
            if (b.isGameTarget(internal)) {
                InjectProgress.classStart(b.displayName(internal));
                byte[] out = b.transformGameClass(internal, classBytes);
                if (out != null) {
                    PigLog.log("[Pig] 已注入 " + b.displayName(internal));
                    return out;
                }
            }
            if (NamespaceRegistry.isOwnClass(internal) && !internal.startsWith(AGENT_PREFIX)) {
                byte[] out = b.transformOwnClass(classBytes);
                if (out != null && out != classBytes) {
                    return out;
                }
            }
        } catch (Throwable t) {
            PigLog.err("[Pig] NativeBridge.transform 失败 " + internal + ": " + t);
        }
        return null;
    }

                                                            
    public static boolean isTarget(String internalName) {
        NamespaceBridge b = bridge;
        return b != null && b.isGameTarget(internalName);
    }

       
                                                       
                                                                 
                                                      
       
    private static NamespaceBridge correctKind(String internal, byte[] classBytes) {
        NamespaceBridge b = bridge;
        if (b == null || kindCorrected || !internal.startsWith("net/minecraft/")) {
            return b;
        }
        kindCorrected = true;
        if (b.runtime() != MappingKind.NOTCH) {
            return b;
        }
        MappingKind kind = NamespaceDetector.detectFromBytes(classBytes);
        synchronized (NativeBridge.class) {
            if (bridge != null && bridge.runtime() == MappingKind.NOTCH) {
                preloadInfra(kind);
                bridge = new NamespaceBridge(kind);
                PigLog.log("[Pig] 命名空间修正为 " + kind + "（类名已是 net/minecraft/**）");
                return bridge;
            }
        }
        return bridge;
    }

                              
    public static String mcpName(String internalName) {
        NamespaceBridge b = bridge;
        if (b == null || internalName == null) {
            return internalName;
        }
        try {
            String name = b.displayName(internalName);
            return name == null ? internalName : name;
        } catch (Throwable t) {
            return internalName;
        }
    }

                                                        
    public static void bootstrap() {
        PigLog.log("[Pig] bootstrap 开始，查找注入入口");
        final String entry = locateEntry();
        if (entry == null || entry.isEmpty()) {
            PigLog.err("[Pig] 未发现注入入口：清单无 Pig-Entry 且未扫描到 @PigEntry"
                    + "（jar=" + selfJarPath + "）——注入不会生效");
            InjectProgress.fail("Injection failed");
            return;
        }
        PigLog.log("[Pig] 自动发现注入入口: " + entry);
        PigLog.log("[Pig] 检查游戏实例是否就绪");
        if (minecraftReady()) {
            PigLog.log("[Pig] 游戏已就绪，直接启动入口");
            startEntry(entry);
            return;
        }
                                              
                                                   
        PigLog.log("[Pig] 游戏尚未就绪，等待 Minecraft 初始化后再启动注入入口");
        Thread wait = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 900; i++) {             
                    if (minecraftReady()) {
                        break;
                    }
                    if (i == 75) {
                                                              
                        PigLog.err("[Pig] 15s 仍拿不到 Minecraft 实例，直接尝试启动入口");
                        startEntry(entry);
                        return;
                    }
                    if (i > 0 && i % 25 == 0) {
                        PigLog.log("[Pig] 仍在等待 Minecraft 实例（" + (i / 5) + "s）");
                    }
                    try {
                        Thread.sleep(200L);
                    } catch (InterruptedException ignored) {
                    }
                }
                startEntry(entry);
            }
        }, "Pig-BootstrapWait");
        wait.setDaemon(true);
        wait.start();
    }

    private static String locateEntry() {
        String entry = null;
                                                                            
        if (selfJarPath != null && !selfJarPath.isEmpty()) {
            entry = EntryLocator.locateIn(new File(selfJarPath));
            if (entry == null || entry.isEmpty()) {
                PigLog.err("[Pig] 在 DLL 指定 jar 中未发现入口，回退自省定位: " + selfJarPath);
                entry = null;
            }
        }
        if (entry == null || entry.isEmpty()) {
            entry = EntryLocator.locate();
        }
        return entry;
    }

    private static void startEntry(String entry) {
        try {
            PigLog.log("[Pig] 正在启动注入入口: " + entry);
            Class<?> rt = Class.forName(entry, true, NativeBridge.class.getClassLoader());
            rt.getMethod("start").invoke(null);
            PigLog.log("[Pig] 注入入口已启动: " + entry);
        } catch (Throwable t) {
            PigLog.err("[Pig] 启动注入入口失败", t);
            InjectProgress.fail("Injection failed");
        }
    }

       
                          
                                                            
                               
       
    private static boolean minecraftReady() {
        try {
            NamespaceBridge b = bridge;
            if (b == null) {
                return false;
            }
            NamespaceRegistry reg = NamespaceRegistry.fromMcp(b.runtime());
            String internal = "net/minecraft/client/Minecraft";
            String dotted = reg.mapClassName(internal).replace('/', '.');
            java.lang.reflect.Method find = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            find.setAccessible(true);
            Object mcClass = null;
                                             
            ClassLoader own = NativeBridge.class.getClassLoader();
            ClassLoader ctx = Thread.currentThread().getContextClassLoader();
            ClassLoader[] roots = new ClassLoader[]{gameLoader, own, ctx};
            for (ClassLoader root : roots) {
                for (ClassLoader l = root; l != null && mcClass == null; l = l.getParent()) {
                    mcClass = find.invoke(l, dotted);
                }
                if (mcClass != null) {
                    break;
                }
            }
            if (mcClass == null) {
                if (!mcMissLogged) {
                    mcMissLogged = true;
                    PigLog.err("[Pig] 未在任何类加载器里找到 " + dotted
                            + "（gameLoader=" + gameLoader + "）");
                }
                return false;
            }
            String getMinecraft = reg.remapper().mapMethodName(internal, "getMinecraft",
                    "()Lnet/minecraft/client/Minecraft;");
                                           
            String[] names = new String[]{getMinecraft, "getMinecraft", "func_71410_x"};
            for (String name : names) {
                try {
                    Object mc = ((Class<?>) mcClass).getMethod(name).invoke(null);
                    if (mc != null) {
                        return true;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (!mcMissLogged) {
                mcMissLogged = true;
                PigLog.err("[Pig] 找到 Minecraft 类但取不到实例，候选方法=" + names[0] + "/" + names[1] + "/" + names[2]);
            }
            return false;
        } catch (Throwable t) {
            if (!mcMissLogged) {
                mcMissLogged = true;
                PigLog.err("[Pig] minecraftReady 异常", t);
            }
            return false;
        }
    }

       
                                                                
                                    
                                                
       
    private static void preloadInfra(MappingKind kind) {
        ClassLoader loader = NativeBridge.class.getClassLoader();
                                                      
        loadQuietly(loader, MappingKind.class);
        loadQuietly(loader, NamespaceRegistry.class);
                                                                               
                                                                                
                                                          
        loadQuietly(loader, cn.pig.asm.TransformDispatcher.class);
        try {
            NamespaceRegistry.fromMcp(kind);
            NamespaceRegistry.toMcp(kind);
        } catch (Throwable t) {
        }
    }

    private static void loadQuietly(ClassLoader loader, Class<?> type) {
        try {
            Class.forName(type.getName(), true, loader);
        } catch (Throwable t) {
        }
    }

                                                                 
    private static MappingKind parseKind(String options) {
        if (options == null) {
            return null;
        }
        String value = options.trim().toLowerCase();
        if (value.isEmpty() || value.contains("auto")) {
            return null;
        }
        if (value.contains("srg")) {
            return MappingKind.SRG;
        }
        if (value.contains("notch")) {
            return MappingKind.NOTCH;
        }
        if (value.contains("mcp")) {
            return MappingKind.MCP;
        }
        return null;
    }
}
