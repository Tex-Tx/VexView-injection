package cn.pig.asm;

public class CallbackInfo {
    private final String name;
    private boolean cancelled;

    public CallbackInfo(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
