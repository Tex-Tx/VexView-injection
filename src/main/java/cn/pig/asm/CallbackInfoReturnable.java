package cn.pig.asm;

public class CallbackInfoReturnable<R> {
    private final String name;
    private boolean cancelled;
    private R returnValue;

    public CallbackInfoReturnable(String name, R defaultValue) {
        this.name = name;
        this.returnValue = defaultValue;
    }

    public String name() {
        return name;
    }

    public void cancel() {
        cancelled = true;
    }

    public void setReturnValue(R value) {
        returnValue = value;
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public R getReturnValue() {
        return returnValue;
    }
}
