package cn.pig.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

   
                                       
                                                          
                       
   
public final class PigLog {

    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT);
    private static File file;

    private PigLog() {
    }

    public static synchronized void useJar(String jarPath) {
        try {
            file = new File(System.getProperty("java.io.tmpdir", "."), "zuro-inject.log");
        } catch (Throwable ignored) {
        }
                                          
        InjectProgress.start();
    }

    public static synchronized void log(String msg) {
        if (msg == null) {
            return;
        }
        write("J", msg);
        if (msg.startsWith("[Pig] 已注入 ")) {
            InjectProgress.classDone(msg.substring("[Pig] 已注入 ".length()).trim());
        } else if (msg.contains("注入入口已启动")) {
            InjectProgress.finish();
        } else if (msg.contains("安装完成")) {
            InjectProgress.phase("Preparing classes");
        } else if (msg.contains("等待 Minecraft 初始化")) {
            InjectProgress.phase("Waiting for game");
        } else if (msg.contains("PigRuntime 开始初始化")) {
            InjectProgress.phase("Starting client");
        }
    }

    public static synchronized void err(String msg) {
        write("E", msg);
    }

                        
    public static synchronized void err(String msg, Throwable t) {
        write("E", msg + (t == null ? "" : ": " + t));
        if (t != null && file != null) {
            try {
                java.io.PrintWriter w = new java.io.PrintWriter(
                        new java.io.OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"), true);
                t.printStackTrace(w);
                w.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private static void write(String tag, String msg) {
        File f = file;
        if (f == null) {
            return;
        }
        try {
            FileOutputStream out = new FileOutputStream(f, true);
            try {
                OutputStreamWriter w = new OutputStreamWriter(out, "UTF-8");
                w.write("[" + TIME.format(new Date()) + "] " + tag + " " + msg + "\r\n");
                w.flush();
            } finally {
                out.close();
            }
        } catch (Throwable ignored) {
        }
    }
}
