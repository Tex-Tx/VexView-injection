package cn.pig.agent;

import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;

   
                           
  
                                                                    
                                                                              
                                                
   
public final class InjectProgress {

    private static final int WIDTH = 520;
    private static final int HEIGHT = 210;
                                           
    private static final long MIN_VISIBLE_MS = 1500L;
    private static final Color BACKGROUND = new Color(0, 0, 0);
    private static final Color TEXT = Color.WHITE;
    private static final Color MUTED = new Color(190, 190, 190);
    private static final Color BAR_BG = new Color(38, 38, 38);
    private static final Color BAR_FG = Color.WHITE;

    private static volatile boolean started;
    private static volatile boolean finished;
    private static volatile String phase = "Connecting to game process...";
    private static volatile String currentClass = "";
    private static volatile int classes;
    private static volatile double progress = 0.04;
    private static volatile long shownAt;
    private static volatile JWindow window;
    private static volatile Panel panel;

    private InjectProgress() {
    }

                                                  
    public static void start() {
        if (started || GraphicsEnvironment.isHeadless()) {
            return;
        }
        synchronized (InjectProgress.class) {
            if (started) {
                return;
            }
            started = true;
        }
        try {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    createWindow();
                }
            });
                                                    
            Thread watchdog = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(180000L);
                    } catch (InterruptedException ignored) {
                    }
                    if (!finished) {
                        fail("Injection timed out");
                    }
                }
            }, "Pig-InjectProgressWatchdog");
            watchdog.setDaemon(true);
            watchdog.start();
        } catch (Throwable ignored) {
        }
    }

    private static void createWindow() {
        try {
            Panel content = new Panel();
            content.setPreferredSize(new Dimension(WIDTH, HEIGHT));
            content.setBackground(BACKGROUND);

            JWindow w = new JWindow();
            w.setBackground(BACKGROUND);
            w.setContentPane(content);
            w.setAlwaysOnTop(true);
                                        
            w.setFocusableWindowState(false);
            w.setAutoRequestFocus(false);
            w.pack();
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            w.setLocation((screen.width - WIDTH) / 2, (screen.height - HEIGHT) / 2);
            w.setVisible(true);

            shownAt = System.currentTimeMillis();
            panel = content;
            window = w;
            content.repaint();
        } catch (Throwable ignored) {
        }
    }

                    
    public static void phase(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        phase = text;
        if (progress < 0.20) {
            progress = 0.20;
        }
        repaint();
    }

                             
    public static void classStart(String className) {
        if (className == null || className.isEmpty()) {
            return;
        }
        currentClass = className;
        repaint();
    }

                         
    public static void classDone(String className) {
        if (className != null && !className.isEmpty()) {
            currentClass = className;
        }
        classes++;
                                   
        double p = 0.25 + 0.70 * (1.0 - 1.0 / (1.0 + classes / 12.0));
        if (p > progress) {
            progress = p;
        }
        repaint();
    }

                                  
    public static void finish() {
        if (finished) {
            return;
        }
        finished = true;
        progress = 1.0;
        phase = "Injection complete";
        repaint();
        closeLater(700L);
    }

                            
    public static void fail(String reason) {
        if (finished) {
            return;
        }
        finished = true;
        phase = reason == null || reason.isEmpty() ? "Injection failed" : reason;
        repaint();
        closeLater(1500L);
    }

    private static void closeLater(final long delayMs) {
        long wait = delayMs;
        long visible = System.currentTimeMillis() - shownAt;
        if (shownAt > 0 && visible < MIN_VISIBLE_MS) {
            wait = Math.max(wait, MIN_VISIBLE_MS - visible);
        }
        final long delay = wait;
        Thread closer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {
                }
                dismiss();
            }
        }, "Pig-InjectProgressClose");
        closer.setDaemon(true);
        closer.start();
    }

    private static void dismiss() {
        final JWindow w = window;
        window = null;
        panel = null;
        if (w == null) {
            return;
        }
        try {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        w.dispose();
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static void repaint() {
        final Panel p = panel;
        if (p == null) {
            return;
        }
        try {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    p.repaint();
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static final class Panel extends JPanel {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int width = getWidth();
            int height = getHeight();
            g.setColor(BACKGROUND);
            g.fillRect(0, 0, width, height);
            g.setColor(new Color(70, 70, 70));
            g.drawRect(0, 0, width - 1, height - 1);

            g.setFont(new Font("Segoe UI", Font.BOLD, 26));
            g.setColor(TEXT);
            g.drawString("Zuro injection", 28, 56);

            g.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            g.setColor(TEXT);
            g.drawString(phase, 30, 92);

                                
            g.setColor(MUTED);
            g.setFont(new Font("Consolas", Font.PLAIN, 13));
            g.drawString(ellipsis(g, currentClass, 440), 30, 118);

            double value = progress;
            if (value < 0.0) {
                value = 0.0;
            } else if (value > 1.0) {
                value = 1.0;
            }
                               
            g.setColor(BAR_BG);
            g.fillRect(30, 142, 400, 12);
            int filled = (int) Math.round(400 * value);
            if (filled > 0) {
                g.setColor(BAR_FG);
                g.fillRect(30, 142, Math.min(400, filled), 12);
            }
            g.setColor(TEXT);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            g.drawString((int) Math.round(value * 100.0) + "%", 440, 154);
        }

                                             
        private String ellipsis(Graphics g, String text, int maxWidth) {
            if (text == null || text.isEmpty()) {
                return "";
            }
            if (g.getFontMetrics().stringWidth(text) <= maxWidth) {
                return text;
            }
            String tail = text;
            while (tail.length() > 1 && g.getFontMetrics().stringWidth("..." + tail) > maxWidth) {
                tail = tail.substring(1);
            }
            return "..." + tail;
        }
    }
}
