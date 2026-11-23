package cn.pig.runtime;

import cn.pig.agent.NamespaceDetector;
import cn.pig.agent.PigLog;
import cn.pig.asm.ns.MappingKind;
import cn.pig.asm.ns.NamespaceRegistry;
import org.objectweb.asm.commons.Remapper;

   
                                            
  
                                                
                                                    
                                                                
   
public final class VexRuntime {

    private static boolean started;

    private VexRuntime() {
    }

    public static synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        Runnable init = new Runnable() {
            @Override
            public void run() {
                initClient();
            }
        };
        if (!scheduleOnClientThread(init)) {
            PigLog.err("[VexView] 无法调度到游戏主线程，直接在注入线程初始化");
            init.run();
        }
    }

    private static void initClient() {
        try {
            PigLog.log("[VexView] 开始初始化");
            ClassLoader loader = VexRuntime.class.getClassLoader();
            Class<?> modClass = Class.forName("lk.vexview.VexView", true, loader);
            Object mod = modClass.getDeclaredConstructor().newInstance();
            try {
                modClass.getField("s11").set(null, mod);
            } catch (Throwable ignored) {
            }

                                                            
            try {
                VexStaticTextures.install();
            } catch (Throwable t) {
                PigLog.err("[VexView] 静态贴图注册失败: " + t);
            }

            Class<?> preClass = cn.pig.compat.fml.common.event.FMLPreInitializationEvent.class;
            Object preEvent = preClass.getDeclaredConstructor().newInstance();
            modClass.getMethod("preload", preClass).invoke(mod, preEvent);

            Class<?> initClass = cn.pig.compat.fml.common.event.FMLInitializationEvent.class;
            Object initEvent = initClass.getDeclaredConstructor().newInstance();
            modClass.getMethod("init", initClass).invoke(mod, initEvent);

            PigLog.log("[VexView] 初始化完成");
        } catch (Throwable t) {
            Throwable root = t;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            java.io.StringWriter buffer = new java.io.StringWriter();
            root.printStackTrace(new java.io.PrintWriter(buffer));
            PigLog.err("[VexView] 初始化失败: " + root + "\n" + buffer.toString());
        }
    }

                                                         
    private static boolean scheduleOnClientThread(Runnable task) {
        try {
            ClassLoader loader = VexRuntime.class.getClassLoader();
            MappingKind kind = NamespaceDetector.detect(loader);
            NamespaceRegistry registry = NamespaceRegistry.fromMcp(kind);
            Remapper remapper = registry.remapper();
            String internal = "net/minecraft/client/Minecraft";
            String dotted = registry.mapClassName(internal).replace('/', '.');
            Class<?> mcClass = Class.forName(dotted, false, loader);
            String getMinecraft = remapper.mapMethodName(internal, "getMinecraft",
                    "()Lnet/minecraft/client/Minecraft;");
            Object mc = mcClass.getMethod(getMinecraft).invoke(null);
            String addTask = remapper.mapMethodName(internal, "addScheduledTask",
                    "(Ljava/lang/Runnable;)Lcom/google/common/util/concurrent/ListenableFuture;");
            mcClass.getMethod(addTask, Runnable.class).invoke(mc, task);
            PigLog.log("[VexView] 已调度初始化到游戏主线程");
            return true;
        } catch (Throwable t) {
            PigLog.err("[VexView] 调度主线程失败: " + t);
            return false;
        }
    }
}
